package com.unifiedmedia.cms.service;

import com.unifiedmedia.cms.entity.*;
import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.nodes.io.JobSourceScannerNode;
import com.unifiedmedia.cms.plugin.NodeRegistry;
import com.unifiedmedia.cms.pipeline.core.VfsFile;
import com.unifiedmedia.cms.pipeline.payload.PipelineKeys;
import com.unifiedmedia.cms.pipeline.spi.PipelineEventListener;
import com.unifiedmedia.cms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchJobService {

    private final NodeRegistry nodeRegistry;
    private final BatchJobRepository jobRepository;
    private final TaskRepository taskRepository;
    private final TemplateRepository templateRepository;
    private final TaskNodeLogRepository taskNodeLogRepository;
    private final List<PipelineEventListener> eventListeners;

    private final FileTaskLifecycleManager fileTaskLifecycleManager;
    private final Executor pipelineTaskExecutor;
    private final TransactionTemplate transactionTemplate;
    private final PipelineGraphParser graphParser;
    private final JobSourceScannerNode sourceScanner;

    private final Map<String, PipelineNode> nodeMap = new ConcurrentHashMap<>();
    private final Map<UUID, PipelineGraphParser.DagResult> graphCache = new ConcurrentHashMap<>();

    private Map<String, PipelineNode> getNodeMap() {
        if (nodeMap.isEmpty() || nodeMap.size() != nodeRegistry.getNodes().size()) {
            nodeMap.clear();
            for (PipelineNode n : nodeRegistry.getNodes()) nodeMap.put(NodeMetaReader.getNodeName(n), n);
        }
        return nodeMap;
    }

    // ==================== 提交作业 ====================

    @Transactional
    public BatchJob submitJob(String name, UUID templateId, Map<String, Object> inputPath, Map<String, Object> outputPath) {
        String jobName = (name != null && !name.isBlank()) ? name.trim() : ("Job-" + System.currentTimeMillis());
        if (jobRepository.existsByName(jobName)) throw new IllegalArgumentException("名称已存在: " + jobName);

        Template t = (templateId != null) ? templateRepository.findById(templateId).orElseThrow()
                : templateRepository.findByIsDefaultTrue().orElseThrow(() -> new IllegalArgumentException("没有默认模板"));
        Map<String, Object> graph = t.getGraphPayload();
        if (graph == null || graph.isEmpty()) throw new IllegalArgumentException("模板无 graph_payload");
        graphParser.parseAndValidate(graph); // 提交时强制校验，防死循环恶意发包

        BatchJob job = BatchJob.builder().name(jobName).templateId(t.getId()).status("PENDING").executionGraph(graph).build();
        if (inputPath != null && !inputPath.isEmpty()) {
            Object sid = inputPath.get("storage_node_id");
            if (sid != null && !sid.toString().isBlank()) job.setInputStorageNodeId(UUID.fromString(sid.toString()));
            Object p = inputPath.get("path"); if (p != null) job.setInputPathText(p.toString());
        }
        if (outputPath != null && !outputPath.isEmpty()) {
            Object sid = outputPath.get("storage_node_id");
            if (sid != null && !sid.toString().isBlank()) job.setOutputStorageNodeId(UUID.fromString(sid.toString()));
            Object p = outputPath.get("path"); if (p != null) job.setOutputPathText(p.toString());
        }
        return jobRepository.save(job);
    }

    // ==================== 作业操作 ====================

    @Async
    public void startJob(UUID jobId) {
        // 毫秒级编程式事务：只包裹删除，用完立刻归还连接
        transactionTemplate.executeWithoutResult(status -> {
            List<Task> oldTasks = taskRepository.findByJobIdOrderByCreatedAtAsc(jobId);
            List<UUID> oldTaskIds = oldTasks.stream().map(Task::getId).toList();
            if (!oldTaskIds.isEmpty()) taskNodeLogRepository.deleteByTaskIdIn(oldTaskIds);
            taskRepository.deleteByJobId(jobId);
        });
        // 无事务环境，安全执行并发分发与 join 等待
        executeJob(jobId);
    }

    @Async
    public void continueJob(UUID jobId) { executeJob(jobId); }

    @Async
    public void retryTask(UUID taskId) {
        Task ft = taskRepository.findById(taskId).orElseThrow();
        BatchJob job = jobRepository.findById(ft.getJobId()).orElseThrow();

        // 1. 前置清理：删除旧日志 + 广播清空事件
        taskNodeLogRepository.deleteByTaskId(taskId);
        for (var listener : eventListeners) {
            listener.onNodeLogsCleared(taskId, job.getId());
        }

        // 2. 校验文件路径
        if (ft.getFilePath() == null) {
            ft.setStatus("FAILED");
            ft.setErrorMessage("File path missing, cannot retry");
            taskRepository.save(ft);
            for (var listener : eventListeners) {
                listener.onTaskStatusChanged(taskId, job.getId(), "FAILED", "文件路径缺失");
            }
            return;
        }

        java.io.File file = new java.io.File(ft.getFilePath());
        if (!file.exists()) {
            ft.setStatus("FAILED");
            ft.setErrorMessage("文件不存在: " + ft.getFilePath());
            taskRepository.save(ft);
            for (var listener : eventListeners) {
                listener.onTaskStatusChanged(taskId, job.getId(), "FAILED", "文件不存在: " + ft.getFilePath());
            }
            return;
        }

        // 3. 构建 VfsFile
        VfsFile candidate = VfsFile.builder()
                .remotePath(ft.getFilePath())
                .fileName(file.getName())
                .mimeType(null)
                .format(MediaFormat.fromExtension(ft.getFilePath()))
                .build();

        // 4. 解析执行图
        PipelineGraphParser.DagResult parsed = getOrParseGraph(job.getId(), job.getExecutionGraph());
        if (parsed == null || parsed.nodes().isEmpty()) {
            ft.setStatus("FAILED");
            ft.setErrorMessage("执行图为空");
            taskRepository.save(ft);
            for (var listener : eventListeners) {
                listener.onTaskStatusChanged(taskId, job.getId(), "FAILED", "执行图为空");
            }
            return;
        }

        // 5. 提取全局配置
        Map<String, Object> globalConfigs = extractGlobalConfigs(job);
        Map<String, PipelineNode> nm = new HashMap<>(getNodeMap());

        // 6. 委托 FileTaskLifecycleManager 执行
        try {
            fileTaskLifecycleManager.executeFilePipeline(job.getId(), candidate, globalConfigs, parsed.nodes(), parsed.sorted(), nm);
            log.info("[BatchJob] retry task {} completed for file: {}", taskId, ft.getFilePath());
        } catch (Exception e) {
            log.error("[BatchJob] retry task {} failed: {}", taskId, e.getMessage());
        }
    }

    @Transactional
    public void resetJob(UUID jobId) {
        List<Task> oldTasks = taskRepository.findByJobIdOrderByCreatedAtAsc(jobId);
        List<UUID> oldTaskIds = oldTasks.stream().map(Task::getId).toList();
        if (!oldTaskIds.isEmpty()) taskNodeLogRepository.deleteByTaskIdIn(oldTaskIds);
        taskRepository.deleteByJobId(jobId);
        BatchJob j = jobRepository.findById(jobId).orElseThrow();
        j.setStatus("PENDING"); jobRepository.save(j);
    }

    @Transactional
    public void stopJob(UUID jobId) {
        BatchJob j = jobRepository.findById(jobId).orElseThrow();
        if (!"RUNNING".equals(j.getStatus())) throw new IllegalStateException("作业未在运行");
        j.setStatus("FAILED"); jobRepository.save(j);
    }

    @Transactional
    public void updateJob(UUID jobId, Map<String, Object> body) {
        BatchJob j = jobRepository.findById(jobId).orElseThrow();
        if ("RUNNING".equals(j.getStatus())) throw new IllegalStateException("运行中不可编辑");

        if (body.containsKey("name")) {
            String nn = ((String) body.get("name")).trim();
            if (jobRepository.existsByNameAndIdNot(nn, jobId)) throw new IllegalArgumentException("名称已存在");
            j.setName(nn);
        }
        if (body.containsKey("inputPathText")) j.setInputPathText((String) body.get("inputPathText"));
        if (body.containsKey("outputPathText")) j.setOutputPathText((String) body.get("outputPathText"));
        if (body.containsKey("inputStorageNodeId")) {
            Object sid = body.get("inputStorageNodeId");
            j.setInputStorageNodeId(sid != null && !sid.toString().isBlank() ? UUID.fromString(sid.toString()) : null);
        }
        if (body.containsKey("outputStorageNodeId")) {
            Object sid = body.get("outputStorageNodeId");
            j.setOutputStorageNodeId(sid != null && !sid.toString().isBlank() ? UUID.fromString(sid.toString()) : null);
        }
        jobRepository.save(j);
    }

    @Transactional
    public void deleteJob(UUID jobId, boolean deleteAssets, boolean deleteSourceFiles) {
        BatchJob j = jobRepository.findById(jobId).orElseThrow();
        if ("RUNNING".equals(j.getStatus())) throw new IllegalStateException("运行中不可删除");
        List<Task> oldTasks = taskRepository.findByJobIdOrderByCreatedAtAsc(jobId);
        List<UUID> oldTaskIds = oldTasks.stream().map(Task::getId).toList();
        if (!oldTaskIds.isEmpty()) taskNodeLogRepository.deleteByTaskIdIn(oldTaskIds);
        taskRepository.deleteByJobId(jobId);
        jobRepository.delete(j);
    }

    // ==================== 执行引擎 ====================

    private void executeJob(UUID jobId) {
        BatchJob job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus("RUNNING");
        jobRepository.save(job);

        PipelineGraphParser.DagResult parsed = getOrParseGraph(jobId, job.getExecutionGraph());
        if (parsed == null || parsed.nodes().isEmpty()) {
            job.setStatus("FAILED");
            jobRepository.save(job);
            return;
        }

        // 使用系统级 JobSourceScannerNode 扫描源目录
        List<VfsFile> files = scanSourceFiles(job);
        if (files.isEmpty()) {
            job.setStatus("FAILED");
            jobRepository.save(job);
            return;
        }

        // Extract global configs
        Map<String, Object> globalConfigs = extractGlobalConfigs(job);
        Map<String, PipelineNode> nm = new HashMap<>(getNodeMap());

        // Concurrent dispatch: each file runs in its own transaction via FileTaskLifecycleManager
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (VfsFile file : files) {
            futures.add(CompletableFuture.runAsync(() -> fileTaskLifecycleManager.executeFilePipeline(jobId, file, globalConfigs, parsed.nodes(), parsed.sorted(), nm), pipelineTaskExecutor));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            job.setStatus("COMPLETED");
            jobRepository.save(job);
            log.info("[BatchJob] {} completed", jobId);
        } catch (Exception e) {
            log.error("[BatchJob] {} failed: {}", jobId, e.getMessage(), e);
            job.setStatus("FAILED");
            jobRepository.save(job);
        }
    }

    /** 使用系统级 JobSourceScannerNode 扫描源目录 */
    private List<VfsFile> scanSourceFiles(BatchJob job) {
        PipelineTaskContext ctx = new PipelineTaskContext(UUID.randomUUID(), null);
        String path = job.getInputPathText();
        if (path != null) {
            if (path.matches(".*\\.(txt|TXT|epub|EPUB|pdf|PDF|mp4|mkv)$"))
                ctx.setPipelineData(PipelineKeys.ABSOLUTE_PATH, path);
            else ctx.setPipelineData(PipelineKeys.SOURCE_DIRECTORY, path);
        }
        if (job.getInputStorageNodeId() != null)
            ctx.setPipelineData(PipelineKeys.STORAGE_NODE_ID, job.getInputStorageNodeId());
        if (job.getOutputPathText() != null)
            ctx.setPipelineData(PipelineKeys.TARGET_PATH, Map.of("storage_node_id",
                    job.getOutputStorageNodeId() != null ? job.getOutputStorageNodeId().toString() : "",
                    "path", job.getOutputPathText()));

        try { sourceScanner.execute(ctx); } catch (Exception e) {
            log.error("[BatchJob] Scanner failed: {}", e.getMessage(), e);
            return List.of();
        }
        return ctx.getPipelineList(PipelineKeys.FILE_CANDIDATES, VfsFile.class);
    }

    // ==================== 工具方法 ====================

    private Map<String, Object> extractGlobalConfigs(BatchJob job) {
        Map<String, Object> globalConfigs = new LinkedHashMap<>();
        if (job.getOutputPathText() != null) {
            globalConfigs.put("targetPath", Map.of("storage_node_id",
                    job.getOutputStorageNodeId() != null ? job.getOutputStorageNodeId().toString() : "",
                    "path", job.getOutputPathText()));
        }
        if (job.getInputStorageNodeId() != null) globalConfigs.put("storageNodeId", job.getInputStorageNodeId());
        if (job.getInputPathText() != null) globalConfigs.put("sourceDirectory", job.getInputPathText());
        return globalConfigs;
    }

    // ==================== 图算法（委托 PipelineGraphParser）====================

    private PipelineGraphParser.DagResult getOrParseGraph(UUID jobId, String graph) {
        return graphCache.computeIfAbsent(jobId, id -> graphParser.parseAndValidate(graph));
    }

    // ==================== 查询 ====================

    public Map<String, Object> getJobsPaged(int page, int size, String search, String status, String sort) {
        var all = jobRepository.findAll();
        var stream = all.stream();
        if (search != null && !search.isBlank()) stream = stream.filter(j -> j.getName() != null && j.getName().contains(search));
        if (status != null && !status.isBlank()) stream = stream.filter(j -> status.equals(j.getStatus()));
        // sort: newest first
        stream = stream.sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        var list = stream.toList();
        int total = list.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        var pageItems = from < total ? list.subList(from, to) : List.<BatchJob>of();
        return Map.of("items", pageItems, "total", total, "page", page, "size", size);
    }

    public Map<String, Object> getPendingCount() {
        long failed = jobRepository.findAll().stream().filter(j -> "FAILED".equals(j.getStatus())).count();
        long pending = jobRepository.findAll().stream().filter(j -> "PENDING".equals(j.getStatus()) || "QUEUED".equals(j.getStatus())).count();
        return Map.of("failed", failed, "pending", pending, "total", failed + pending);
    }

    public Optional<BatchJob> getJob(UUID id) { return jobRepository.findById(id); }
    public List<PipelineNode> getAvailableNodes() { return List.copyOf(nodeRegistry.getNodes()); }
}
