package com.unifiedmedia.cms.service;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.orchestrator.PipelineOrchestrator;
import com.unifiedmedia.cms.pipeline.payload.NodeExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 单文件任务生命周期管理器 — 纯壳子，不含任何业务逻辑或 JPA 依赖。
 * <p>
 * 职责：构建纯净上下文 → 写启动日志 → 调 Orchestrator → 写结果日志 → 沙箱清理。
 * 不干涉 DAG 流转，跳过、夹心饼干等全由 Orchestrator 处理。
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

            List<NodeExecutionResult> results = orchestrator.run(
                    new ArrayList<>(nodeMap.values()), graphNodes, sortedNodeIds, nodeMap, ctx);

            logManager.saveNodeResults(jobId, ctx, results);
            logManager.saveFinalStatus(jobId, ctx);
        } catch (Exception e) {
            log.error("[LifecycleManager] Task failed for file: {}", file.getFileName(), e);
            if (ctx != null) logManager.saveFailLog(ctx.getTaskId(), e.getMessage());
        } finally {
            cleanupSandbox(ctx);
        }
    }

    private void cleanupSandbox(PipelineTaskContext ctx) {
        if (ctx == null) return;
        String path = ctx.getPipelineData("tempSandboxPath", String.class);
        if (path != null) {
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(path)); } catch (Exception ignored) {}
        }
    }
}

