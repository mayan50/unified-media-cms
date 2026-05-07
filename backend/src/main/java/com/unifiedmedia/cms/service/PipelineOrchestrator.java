package com.unifiedmedia.cms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifiedmedia.cms.entity.*;
import com.unifiedmedia.cms.pipeline.ArbitrationRequiredException;
import com.unifiedmedia.cms.pipeline.PipelineNode;
import com.unifiedmedia.cms.pipeline.TaskContext;
import com.unifiedmedia.cms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAG 流水线引擎
 * 基于 graph_payload 的有向无环图执行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineOrchestrator {

    private final List<PipelineNode> allNodes;
    private final PipelineTaskRepository taskRepository;
    private final PipelineTaskLogRepository taskLogRepository;
    private final PipelineTemplateRepository templateRepository;
    private final MediaAssetRepository assetRepository;
    private final ObjectMapper objectMapper;

    private final Map<String, PipelineNode> nodeMap = new ConcurrentHashMap<>();
    private final Map<UUID, ParsedGraph> graphCache = new ConcurrentHashMap<>();

    private record ParsedGraph(List<Map<String, Object>> nodes, List<Map<String, Object>> edges, List<String> sorted) {}

    private Map<String, PipelineNode> getNodeMap() {
        if (nodeMap.isEmpty()) {
            for (PipelineNode node : allNodes) {
                nodeMap.put(node.getNodeName(), node);
            }
        }
        return nodeMap;
    }

    private ParsedGraph getOrParseGraph(UUID taskId, String executionGraph) {
        return graphCache.computeIfAbsent(taskId, id -> {
            Map<String, Object> graph = parseGraph(executionGraph);
            if (graph == null) return null;
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) graph.get("nodes");
            List<Map<String, Object>> edges = (List<Map<String, Object>>) graph.get("edges");
            List<String> sorted = topologicalSort(nodes, edges);
            return new ParsedGraph(nodes, edges, sorted);
        });
    }

    public List<Map<String, Object>> getAvailableNodes() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (PipelineNode node : allNodes) {
            Map<String, Object> info = new HashMap<>();
            info.put("name", node.getNodeName());
            info.put("className", node.getClass().getSimpleName());
            result.add(info);
        }
        return result;
    }

    // ==================== 提交任务 ====================

    @Transactional
    public PipelineTask submitTask(String name, UUID templateId, Map<String, Object> inputPath, Map<String, Object> outputPath) {
        // 名称唯一性校验
        String taskName = (name != null && !name.isBlank()) ? name.trim() : ("Task-" + System.currentTimeMillis());
        if (taskRepository.existsByName(taskName)) {
            throw new IllegalArgumentException("Task name already exists: " + taskName);
        }
        // 查找模板
        PipelineTemplate template;
        if (templateId != null) {
            template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
        } else {
            template = templateRepository.findByIsDefaultTrue()
                    .or(() -> templateRepository.findByName("default-book-pipeline"))
                    .orElseThrow(() -> new IllegalArgumentException("No default template found"));
        }

        // 从 inputPath 提取参数
        UUID storageNodeId = null;
        String relativePath = null;
        String sourceDirectory = null;
        if (inputPath != null && !inputPath.isEmpty()) {
            Object sid = inputPath.get("storage_node_id");
            if (sid != null) storageNodeId = UUID.fromString(sid.toString());
            Object p = inputPath.get("path");
            if (p != null) {
                String path = p.toString();
                // 判断是文件还是目录：以常见扩展名结尾的是文件
                if (path.matches(".*\\.(txt|TXT|epub|EPUB|pdf|PDF|mp4|mkv|mov|avi)$")) {
                    relativePath = path;
                } else {
                    sourceDirectory = path;
                }
            }
        }

        // 创建逻辑资产
        String displayPath = relativePath != null ? Path.of(relativePath).getFileName().toString()
                : (sourceDirectory != null ? sourceDirectory : "pipeline-task");
        MediaAsset asset = MediaAsset.builder()
                .libraryId(UUID.randomUUID())
                .title("Processing: " + displayPath)
                .mediaType("BOOK")
                .status("PROCESSING")
                .build();
        assetRepository.save(asset);

        // 快照模板的 graph_payload 到任务
        String executionGraph = template.getGraphPayload();
        if (executionGraph == null || executionGraph.isBlank()) {
            throw new IllegalArgumentException("Template has no graph_payload: " + template.getName());
        }

        // 创建任务
        PipelineTask task = PipelineTask.builder()
                .name(taskName)
                .assetId(asset.getId())
                .currentStatus("QUEUED")
                .templateId(template.getId())
                .executionGraph(executionGraph)
                .build();
        taskRepository.save(task);

        // 创建上下文
        TaskContext context = new TaskContext()
                .setTaskId(task.getId())
                .setAssetId(asset.getId())
                .setStorageNodeId(storageNodeId)
                .setRelativePath(relativePath)
                .setSourceDirectory(sourceDirectory);

        // 注入 outputPath
        if (outputPath != null && !outputPath.isEmpty()) {
            context.put("targetPath", outputPath);
        }

        // 保存 input/output path 到任务（供前端回显）
        if (inputPath != null && !inputPath.isEmpty()) {
            Object sid = inputPath.get("storage_node_id");
            if (sid != null) task.setInputStorageNodeId(UUID.fromString(sid.toString()));
            Object p = inputPath.get("path");
            if (p != null) task.setInputPathText(p.toString());
        }
        if (outputPath != null && !outputPath.isEmpty()) {
            Object sid = outputPath.get("storage_node_id");
            if (sid != null) task.setOutputStorageNodeId(UUID.fromString(sid.toString()));
            Object p = outputPath.get("path");
            if (p != null) task.setOutputPathText(p.toString());
        }

        // 序列化 context 存到任务中，等待手动启动
        try {
            task.setTaskContext(objectMapper.writeValueAsString(context));
        } catch (Exception ignored) {}

        taskRepository.save(task);
        return task;
    }

    // ==================== 启动 / 终止 ====================

    @Transactional
    public void startTask(UUID taskId) {
        PipelineTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        if ("RUNNING".equals(task.getCurrentStatus())) {
            throw new IllegalStateException("Task is already running");
        }
        if ("PENDING_MANUAL".equals(task.getCurrentStatus())) {
            throw new IllegalStateException("Task is waiting for manual arbitration. Use resume instead.");
        }
        TaskContext context = deserializeContext(task.getTaskContext());
        if (context == null) {
            throw new IllegalStateException("Task has no saved context");
        }
        context.setTaskId(taskId);
        executeDagAsync(taskId, context);
    }

    @Transactional
    public PipelineTask updateTask(UUID taskId, String name) {
        PipelineTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        if ("RUNNING".equals(task.getCurrentStatus())) {
            throw new IllegalStateException("Cannot edit a running task");
        }
        String newName = name.trim();
        if (taskRepository.existsByNameAndIdNot(newName, taskId)) {
            throw new IllegalArgumentException("Task name already exists: " + newName);
        }
        task.setName(newName);
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(UUID taskId) {
        PipelineTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        if ("RUNNING".equals(task.getCurrentStatus())) {
            throw new IllegalStateException("Cannot delete a running task. Stop it first.");
        }
        taskRepository.delete(task);
    }

    @Transactional
    public void stopTask(UUID taskId) {
        PipelineTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        if (!"RUNNING".equals(task.getCurrentStatus())) {
            throw new IllegalStateException("Task is not running: " + task.getCurrentStatus());
        }
        task.setCurrentStatus("FAILED");
        taskRepository.save(task);
    }

    private TaskContext deserializeContext(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, TaskContext.class);
        } catch (Exception e) {
            log.error("[Orchestrator] Failed to deserialize context", e);
            return null;
        }
    }

    // ==================== 仲裁恢复 ====================

    @Transactional
    public void resumeTask(UUID taskId, Map<String, Object> mergedPayload) {
        PipelineTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setCurrentStatus("RUNNING");
        taskRepository.save(task);

        TaskContext context = rebuildContext(taskId, mergedPayload);

        // 从 stuck_node_id 继续
        String stuckNodeId = task.getStuckNodeId();
        executeDagFromNode(task.getId(), context, stuckNodeId);
    }

    // ==================== 查询 ====================

    public Optional<PipelineTask> getTask(UUID taskId) {
        return taskRepository.findById(taskId);
    }

    public List<PipelineTaskLog> getTaskLogs(UUID taskId) {
        return taskLogRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    public List<PipelineTask> getAllTasks() {
        return taskRepository.findAll();
    }

    public List<PipelineTask> getTasksByStatus(String status) {
        return taskRepository.findByCurrentStatus(status);
    }

    // ==================== DAG 执行核心 ====================

    @Async
    protected void executeDagAsync(UUID taskId, TaskContext context) {
        executeDagFromNode(taskId, context, null);
    }

    /**
     * 从指定节点开始执行 DAG
     * @param startNodeId 从此节点开始（含），null 表示从头开始
     */
    protected void executeDagFromNode(UUID taskId, TaskContext context, String startNodeId) {
        PipelineTask task = taskRepository.findById(taskId).orElseThrow();
        task.setCurrentStatus("RUNNING");
        task.setStuckNodeId(null);
        taskRepository.save(task);

        // 解析执行图（首次解析后缓存）
        ParsedGraph parsed = getOrParseGraph(taskId, task.getExecutionGraph());
        if (parsed == null || parsed.nodes == null || parsed.nodes.isEmpty()) {
            log.error("[Orchestrator] Invalid execution graph for task {}", taskId);
            task.setCurrentStatus("FAILED");
            taskRepository.save(task);
            return;
        }

        List<Map<String, Object>> nodes = parsed.nodes;
        List<Map<String, Object>> edges = parsed.edges;
        List<String> sorted = parsed.sorted;

        // 如果指定了 startNodeId，从该节点开始
        boolean started = (startNodeId == null);
        Set<String> executed = new HashSet<>();

        for (String nodeId : sorted) {
            if (!started) {
                if (nodeId.equals(startNodeId)) {
                    started = true;
                } else {
                    continue;
                }
            }

            // 查找节点定义
            Map<String, Object> nodeDef = findNodeDef(nodes, nodeId);
            String nodeName = (String) nodeDef.get("name");
            PipelineNode node = getNodeMap().get(nodeName);

            if (node == null) {
                log.warn("[Orchestrator] Node not found: {}, skipping", nodeName);
                saveLog(taskId, nodeName, "SKIPPED", null, "Node not found in registry", null);
                continue;
            }

            // 注入节点配置到 context
            injectNodeConfig(nodeDef, context);

            // 检查前置条件
            if (!node.canExecute(context)) {
                log.info("[Orchestrator] Node {} skipped (canExecute=false)", nodeName);
                saveLog(taskId, nodeName, "SKIPPED", null, "Precondition not met", null);
                executed.add(nodeId);
                continue;
            }

            // 执行节点
            long startTime = System.currentTimeMillis();
            try {
                log.info("[Orchestrator] Executing node: {} (id={})", nodeName, nodeId);
                node.execute(context);
                long elapsed = System.currentTimeMillis() - startTime;
                saveLog(taskId, nodeName, "SUCCESS", extractContextSnapshot(context, nodeName), null, elapsed);
                executed.add(nodeId);
                log.info("[Orchestrator] Node {} completed in {}ms", nodeName, elapsed);

            } catch (ArbitrationRequiredException e) {
                long elapsed = System.currentTimeMillis() - startTime;
                saveLog(taskId, nodeName, "FAILED", e.getArbitrationPayload(), e.getMessage(), elapsed);

                task.setCurrentStatus("PENDING_MANUAL");
                task.setStuckNodeId(nodeId);
                taskRepository.save(task);

                assetRepository.findById(context.getAssetId()).ifPresent(asset -> {
                    asset.setStatus("PENDING_MANUAL");
                    assetRepository.save(asset);
                });

                log.info("[Orchestrator] Task suspended for arbitration at node: {} (id={})", nodeName, nodeId);
                return;

            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                saveLog(taskId, nodeName, "FAILED", null, e.getMessage(), elapsed);

                task.setCurrentStatus("FAILED");
                task.setStuckNodeId(nodeId);
                taskRepository.save(task);

                assetRepository.findById(context.getAssetId()).ifPresent(asset -> {
                    asset.setStatus("PROCESSING");
                    assetRepository.save(asset);
                });

                log.error("[Orchestrator] Node {} failed: {}", nodeName, e.getMessage(), e);
                return;
            }
        }

        // 全部完成
        task.setCurrentStatus("COMPLETED");
        task.setStuckNodeId(null);
        taskRepository.save(task);

        assetRepository.findById(context.getAssetId()).ifPresent(asset -> {
            asset.setStatus("COMPLETED");
            assetRepository.save(asset);
        });

        log.info("[Orchestrator] Pipeline completed for task: {}", taskId);
    }

    // ==================== 图工具方法 ====================

    /**
     * 拓扑排序 (Kahn's algorithm)
     */
    private List<String> topologicalSort(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        Map<String, List<String>> adj = new HashMap<>();

        // 初始化
        for (Map<String, Object> node : nodes) {
            String id = (String) node.get("id");
            inDegree.put(id, 0);
            adj.put(id, new ArrayList<>());
        }

        // 构建邻接表和入度
        for (Map<String, Object> edge : edges) {
            String source = (String) edge.get("source");
            String target = (String) edge.get("target");
            if (adj.containsKey(source) && inDegree.containsKey(target)) {
                adj.get(source).add(target);
                inDegree.merge(target, 1, Integer::sum);
            }
        }

        // Kahn's
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) queue.add(e.getKey());
        }

        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String node = queue.poll();
            result.add(node);
            for (String next : adj.getOrDefault(node, List.of())) {
                int newDeg = inDegree.get(next) - 1;
                inDegree.put(next, newDeg);
                if (newDeg == 0) queue.add(next);
            }
        }

        // 环检测：排序结果不完整则抛异常
        if (result.size() != nodes.size()) {
            throw new IllegalStateException("Cycle detected in execution graph");
        }

        return result;
    }

    /**
     * 解析 execution_graph JSON
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseGraph(String graphStr) {
        if (graphStr == null || graphStr.isBlank()) return null;
        try {
            return objectMapper.readValue(graphStr, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("[Orchestrator] Failed to parse execution graph", e);
            return null;
        }
    }

    /**
     * 将节点配置注入 context，供节点读取
     */
    @SuppressWarnings("unchecked")
    private void injectNodeConfig(Map<String, Object> nodeDef, TaskContext context) {
        Object config = nodeDef.get("config");
        if (config instanceof Map cfg) {
            cfg.forEach((k, v) -> context.put((String) k, v));
        }
        Object condition = nodeDef.get("condition");
        if (condition instanceof String c && !c.isBlank()) {
            context.put("routerCondition", c);
        }
    }

    private Map<String, Object> findNodeDef(List<Map<String, Object>> nodes, String nodeId) {
        return nodes.stream()
                .filter(n -> nodeId.equals(n.get("id")))
                .findFirst()
                .orElse(Map.of());
    }

    // ==================== 日志与上下文 ====================

    private void saveLog(UUID taskId, String nodeName, String status, Object payload, String error, Long elapsed) {
        String payloadStr = null;
        if (payload != null) {
            try { payloadStr = objectMapper.writeValueAsString(payload); } catch (Exception ignored) {}
        }
        PipelineTaskLog logEntry = PipelineTaskLog.builder()
                .taskId(taskId)
                .nodeName(nodeName)
                .status(status)
                .outputPayload(payloadStr)
                .errorMessage(error)
                .executionTimeMs(elapsed)
                .build();
        taskLogRepository.save(logEntry);
    }

    private Object extractContextSnapshot(TaskContext context, String nodeName) {
        return Map.of(
                "detectedFormat", context.getDetectedFormat() != null ? context.getDetectedFormat() : "",
                "detectedMimeType", context.getDetectedMimeType() != null ? context.getDetectedMimeType() : "",
                "aiTitle", context.getAiTitle() != null ? context.getAiTitle() : "",
                "aiAuthor", context.getAiAuthor() != null ? context.getAiAuthor() : "",
                "finalTitle", context.getFinalTitle() != null ? context.getFinalTitle() : ""
        );
    }

    private TaskContext rebuildContext(UUID taskId, Map<String, Object> mergedPayload) {
        List<PipelineTaskLog> logs = taskLogRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
        PipelineTask task = taskRepository.findById(taskId).orElseThrow();

        TaskContext context = new TaskContext()
                .setTaskId(taskId)
                .setAssetId(task.getAssetId());

        // 从日志重建上下文
        for (PipelineTaskLog log : logs) {
            if (log.getOutputPayload() != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = objectMapper.readValue(log.getOutputPayload(), Map.class);
                    if (payload.containsKey("detectedFormat")) context.setDetectedFormat((String) payload.get("detectedFormat"));
                    if (payload.containsKey("detectedMimeType")) context.setDetectedMimeType((String) payload.get("detectedMimeType"));
                    if (payload.containsKey("aiTitle")) context.setAiTitle((String) payload.get("aiTitle"));
                    if (payload.containsKey("aiAuthor")) context.setAiAuthor((String) payload.get("aiAuthor"));
                } catch (Exception ignored) {}
            }
        }

        // 合并仲裁数据
        if (mergedPayload.containsKey("title")) context.setFinalTitle((String) mergedPayload.get("title"));
        if (mergedPayload.containsKey("author")) context.setFinalAuthor((String) mergedPayload.get("author"));
        if (mergedPayload.containsKey("summary")) context.setFinalSummary((String) mergedPayload.get("summary"));
        if (mergedPayload.containsKey("coverUrl")) context.setFinalCoverUrl((String) mergedPayload.get("coverUrl"));
        if (mergedPayload.containsKey("publishYear") && mergedPayload.get("publishYear") != null) {
            context.setFinalPublishYear(((Number) mergedPayload.get("publishYear")).intValue());
        }
        if (mergedPayload.containsKey("tags")) {
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) mergedPayload.get("tags");
            context.setFinalTags(tags);
        }

        return context;
    }
}
