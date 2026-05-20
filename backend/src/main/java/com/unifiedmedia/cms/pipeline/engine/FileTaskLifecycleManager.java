package com.unifiedmedia.cms.pipeline.engine;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.core.annotation.NodeDef;
import com.unifiedmedia.cms.pipeline.orchestrator.PipelineOrchestrator;
import com.unifiedmedia.cms.pipeline.payload.NodeExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 单文件任务生命周期管理器 — 纯壳子。
 * <p>
 * 职责：构建上下文 → 写日志 → 组装执行计划 → 调 Orchestrator → 沙箱清理。
 * 动态过滤 INPUT 节点和 hiddenFromUI 节点，不硬编码任何节点名。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileTaskLifecycleManager {

    private final TaskContextFactory contextFactory;
    private final TaskLogManager logManager;
    private final PipelineOrchestrator orchestrator;

    public void executeFilePipeline(UUID jobId, VfsFile file, Map<String, Object> globalConfigs,
                                     List<Map<String, Object>> graphNodes, List<String> sortedNodeIds,
                                     Map<String, PipelineNode> nodeMap) {
        log.info("[LifecycleManager] Processing file: {}", file.getFileName());
        PipelineTaskContext ctx = null;
        try {
            ctx = contextFactory.buildContext(jobId, file, globalConfigs);
            logManager.saveStartLog(ctx.getTaskId(), jobId, ctx, "系统前置检查完成，源文件已就绪");

            List<PipelineNode> plan = buildExecutionPlan(graphNodes, sortedNodeIds, nodeMap);
            List<NodeExecutionResult> results = orchestrator.run(plan, graphNodes, sortedNodeIds, nodeMap, ctx);

            logManager.saveNodeResults(jobId, ctx, results);
            logManager.saveFinalStatus(jobId, ctx);
        } catch (Exception e) {
            log.error("[LifecycleManager] Task failed for file: {}", file.getFileName(), e);
            if (ctx != null) logManager.saveFailLog(ctx.getTaskId(), e.getMessage());
        } finally {
            cleanupSandbox(ctx);
        }
    }

    /** 从 sortedNodeIds 组装执行计划，自动过滤 INPUT 和 hiddenFromUI 节点 */
    private List<PipelineNode> buildExecutionPlan(List<Map<String, Object>> graphNodes,
                                                   List<String> sortedNodeIds,
                                                   Map<String, PipelineNode> nodeMap) {
        List<PipelineNode> plan = new ArrayList<>();
        for (String nodeId : sortedNodeIds) {
            String nodeName = findNodeName(graphNodes, nodeId);
            PipelineNode node = nodeMap.get(nodeName);
            if (node != null && !isSystemOrInputNode(node)) {
                plan.add(node);
            }
        }
        return plan;
    }

    private boolean isSystemOrInputNode(PipelineNode node) {
        NodeDef def = node.getClass().getAnnotation(NodeDef.class);
        if (def != null) {
            if (def.hiddenFromUI()) return true;
            if (def.type() == NodeType.INPUT || def.type() == NodeType.SYSTEM) return true;
        }
        return false;
    }

    private String findNodeName(List<Map<String, Object>> nodes, String id) {
        return nodes.stream().filter(n -> id.equals(n.get("id")))
                .map(n -> (String) n.get("name")).findFirst().orElse(id);
    }

    private void cleanupSandbox(PipelineTaskContext ctx) {
        if (ctx == null) return;
        String path = ctx.getPipelineData("tempSandboxPath", String.class);
        if (path != null) {
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(path)); } catch (Exception ignored) {}
        }
    }
}
