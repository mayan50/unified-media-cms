package com.unifiedmedia.cms.service;

import com.unifiedmedia.cms.entity.*;
import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.FileCandidate;
import com.unifiedmedia.cms.pipeline.payload.NodeLogEvent;
import com.unifiedmedia.cms.pipeline.payload.PipelineKeys;
import com.unifiedmedia.cms.pipeline.spi.MediaDetailLoader;
import com.unifiedmedia.cms.pipeline.spi.PipelineEventListener;
import com.unifiedmedia.cms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.*;

/**
 * 单文件管线执行器 — 每个文件在独立事务中执行，互不干扰。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileTaskExecutor {

    private final TaskRepository taskRepository;
    private final TaskNodeLogRepository taskNodeLogRepository;
    private final AssetRepository assetRepository;
    private final AssetCreatorRepository assetCreatorRepository;
    private final CreatorRepository creatorRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;
    private final BookDetailRepository bookDetailRepository;
    private final List<MediaDetailLoader> detailLoaders;
    private final List<PipelineEventListener> eventListeners;
    private final PipelineEngine pipelineEngine;

    @Transactional
    public void executeFilePipeline(UUID jobId, FileCandidate file, Map<String, Object> globalConfigs,
                                     List<Map<String, Object>> graphNodes, List<String> sortedNodeIds,
                                     Map<String, PipelineNode> nodeMap) {
        log.info("[FileTaskExecutor] Processing file: {}", file.relativePath());

        // 1. 准备纯净上下文 + 创建/复用 Asset
        TaskContext ctx = prepareContext(jobId, file, globalConfigs);

        // 2. PreCheck 日志
        savePreCheckLog(ctx, jobId, "SUCCESS", "系统前置检查完成，源文件已就绪");

        // 3. 构建执行计划（跳过 FileSnifferNode）
        List<PipelineNode> plan = buildExecutionPlan(graphNodes, sortedNodeIds, nodeMap);

        // 4. 委托纯领域引擎执行 DAG 推演（异常时已收集的结果也不丢失）
        List<com.unifiedmedia.cms.pipeline.payload.NodeExecutionResult> nodeResults;
        try {
            nodeResults = pipelineEngine.run(plan, graphNodes, sortedNodeIds, nodeMap, ctx);
        } catch (Exception e) {
            log.error("[FileTaskExecutor] Engine error for {}: {}", file.relativePath(), e.getMessage(), e);
            nodeResults = List.of();
        }

        // 5. 后置：保存每个节点的 TaskNodeLog（保持时间轴颗粒度）
        saveNodeResults(jobId, ctx, nodeResults);

        // 6. 保存最终 Task 状态
        saveFinalTaskRecord(jobId, ctx);
    }

    private List<PipelineNode> buildExecutionPlan(List<Map<String, Object>> graphNodes,
                                                    List<String> sortedNodeIds,
                                                    Map<String, PipelineNode> nodeMap) {
        int skipIdx = -1;
        for (int i = 0; i < sortedNodeIds.size(); i++) {
            if ("FileSnifferNode".equals(findNodeDef(graphNodes, sortedNodeIds.get(i)).get("name"))) {
                skipIdx = i; break;
            }
        }
        List<PipelineNode> plan = new ArrayList<>();
        for (int i = (skipIdx >= 0 ? skipIdx + 1 : 0); i < sortedNodeIds.size(); i++) {
            PipelineNode node = nodeMap.get(findNodeDef(graphNodes, sortedNodeIds.get(i)).get("name"));
            if (node != null) plan.add(node);
        }
        return plan;
    }

    private Map<String, Object> findNodeDef(List<Map<String, Object>> nodes, String id) {
        return nodes.stream().filter(n -> id.equals(n.get("id"))).findFirst().orElse(Map.of());
    }

    private void saveNodeResults(UUID jobId, TaskContext ctx,
                                   List<com.unifiedmedia.cms.pipeline.payload.NodeExecutionResult> results) {
        String fp = ctx.getPipelineData(PipelineKeys.ABSOLUTE_PATH, String.class);
        if (fp == null || results.isEmpty()) return;
        Task t = findOrCreateTask(jobId, fp);
        for (var r : results) {
            String logOutput = null;
            if (!r.logs().isEmpty()) {
                int stepNum = taskNodeLogRepository.findByTaskIdOrderByStartTimeAsc(t.getId()).size() + 1;
                StringBuilder sb = new StringBuilder();
                sb.append(stepNum).append("   ").append(r.nodeLabel()).append("\n");
                for (String l : r.logs()) sb.append("    ").append(l).append("\n");
                logOutput = sb.toString().trim();
            }
            TaskNodeLog nodeLog = TaskNodeLog.builder()
                    .taskId(t.getId()).nodeName(r.nodeName()).nodeLabel(r.nodeLabel())
                    .status(r.status()).durationMs(r.durationMs()).logOutput(logOutput)
                    .startTime(java.time.LocalDateTime.now().minus(r.durationMs(), java.time.temporal.ChronoUnit.MILLIS))
                    .endTime(java.time.LocalDateTime.now()).errorMessage(r.errorMessage()).build();
            taskNodeLogRepository.save(nodeLog);

            var logEvent = new NodeLogEvent(t.getId(), jobId, nodeLog.getId(),
                    r.nodeName(), r.nodeLabel(), r.status(),
                    nodeLog.getStartTime().toString(), nodeLog.getEndTime().toString(),
                    r.durationMs(), logOutput, r.errorMessage());
            for (var listener : eventListeners) {
                listener.onNodeLogCreated(t.getId(), jobId, logEvent);
                listener.onTaskStatusChanged(t.getId(), jobId, t.getStatus(), t.getErrorMessage());
            }
        }
        // Update task with last completed node
        var last = results.get(results.size() - 1);
        if ("SUCCESS".equals(last.status())) t.setLastCompletedNode(last.nodeName());
        t.setStatus(results.stream().anyMatch(r -> "FAILED".equals(r.status())) ? "FAILED" : "SUCCESS");
        taskRepository.save(t);
    }

    private void saveFinalTaskRecord(UUID jobId, TaskContext ctx) {
        String fp = ctx.getPipelineData(PipelineKeys.ABSOLUTE_PATH, String.class);
        if (fp == null) return;
        Task t = findOrCreateTask(jobId, fp);
        t.setAssetId(ctx.getAsset() != null ? ctx.getAsset().getId() : null);
        t.setStatus("SUCCESS");
        taskRepository.save(t);
    }

    private TaskContext prepareContext(UUID jobId, FileCandidate file, Map<String, Object> globalConfigs) {
        // 查找已有 asset 或创建新 asset
        Task prevTask = findExistingFileTask(jobId, file.absolutePath());
        Asset asset;
        if (prevTask != null && prevTask.getAssetId() != null) {
            asset = assetRepository.findById(prevTask.getAssetId()).orElseGet(() -> createAsset(file));
        } else {
            asset = createAsset(file);
        }

        // 预加载 detail
        MediaDetail detail = null;
        for (MediaDetailLoader loader : detailLoaders) {
            if (loader.supports(asset.getMediaType())) {
                detail = loader.load(asset.getId());
                break;
            }
        }

        StandardTaskContext ctx = new StandardTaskContext(UUID.randomUUID(), asset, detail, globalConfigs);

        // 注入文件私有数据
        ctx.setPipelineData(PipelineKeys.ABSOLUTE_PATH, file.absolutePath());
        ctx.setPipelineData(PipelineKeys.RELATIVE_PATH, file.relativePath());
        ctx.setPipelineData(PipelineKeys.DETECTED_MIME_TYPE, file.mimeType());
        ctx.setPipelineData(PipelineKeys.DETECTED_FORMAT, file.format());

        // 预加载已有作者到 pipeline
        List<AssetCreator> existingCreators = assetCreatorRepository.findByAssetId(asset.getId());
        if (!existingCreators.isEmpty()) {
            List<String> authorNames = existingCreators.stream()
                    .filter(ac -> "作者".equals(ac.getRole()))
                    .map(ac -> creatorRepository.findById(ac.getCreatorId()).map(Creator::getName).orElse(null))
                    .filter(Objects::nonNull).toList();
            if (!authorNames.isEmpty()) ctx.setPipelineData(PipelineKeys.AUTHORS, authorNames);
        }

        // 预加载已有标签到 pipeline
        if (!asset.getTags().isEmpty()) {
            ctx.setPipelineData(PipelineKeys.TAGS, asset.getTags().stream().map(Tag::getName).toList());
        }

        return ctx;
    }

    private Asset createAsset(FileCandidate file) {
        Asset a = Asset.builder()
                .title(Path.of(file.absolutePath()).getFileName().toString().replaceAll("\\.[^.]+$", ""))
                .mediaType("BOOK").build();
        return assetRepository.save(a);
    }

    private Task findExistingFileTask(UUID jobId, String filePath) {
        List<Task> tasks = taskRepository.findByJobIdOrderByCreatedAtAsc(jobId);
        for (int i = tasks.size() - 1; i >= 0; i--) {
            if (filePath.equals(tasks.get(i).getFilePath())) return tasks.get(i);
        }
        return null;
    }

    private void savePreCheckLog(TaskContext ctx, UUID jobId, String status, String message) {
        String fp = ctx.getPipelineData(PipelineKeys.ABSOLUTE_PATH, String.class);
        Task t = fp != null ? findOrCreateTask(jobId, fp) : Task.builder().jobId(jobId).filePath(fp).build();
        t.setAssetId(ctx.getAsset() != null ? ctx.getAsset().getId() : null);
        t.setStatus(status);
        if (!"SUCCESS".equals(status)) t.setErrorMessage(message);
        taskRepository.save(t);

        String logOutput = "1   " + "系统前置检查" + "\n    " + ("SUCCESS".equals(status) ? "[OK] " : "[ERR] ") + message;
        TaskNodeLog nodeLog = TaskNodeLog.builder()
                .taskId(t.getId()).nodeName("PreCheck").nodeLabel("系统前置检查")
                .status(status).startTime(java.time.LocalDateTime.now()).endTime(java.time.LocalDateTime.now())
                .durationMs(0L).logOutput(logOutput)
                .errorMessage("FAILED".equals(status) ? message : null).build();
        taskNodeLogRepository.save(nodeLog);

        var logEvent = new NodeLogEvent(t.getId(), jobId, nodeLog.getId(),
                "PreCheck", "系统前置检查", status,
                nodeLog.getStartTime().toString(), nodeLog.getEndTime().toString(),
                0L, logOutput, nodeLog.getErrorMessage());
        for (var listener : eventListeners) {
            listener.onNodeLogCreated(t.getId(), jobId, logEvent);
        }
    }

    private Task findOrCreateTask(UUID jobId, String filePath) {
        List<Task> tasks = taskRepository.findByJobIdOrderByCreatedAtAsc(jobId);
        for (int i = tasks.size() - 1; i >= 0; i--) {
            if (filePath.equals(tasks.get(i).getFilePath())) return tasks.get(i);
        }
        return Task.builder().jobId(jobId).filePath(filePath).build();
    }
}
