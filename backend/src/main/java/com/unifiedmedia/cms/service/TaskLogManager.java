package com.unifiedmedia.cms.service;

import com.unifiedmedia.cms.entity.Task;
import com.unifiedmedia.cms.entity.TaskNodeLog;
import com.unifiedmedia.cms.pipeline.core.PipelineTaskContext;
import com.unifiedmedia.cms.pipeline.payload.NodeExecutionResult;
import com.unifiedmedia.cms.pipeline.payload.NodeLogEvent;
import com.unifiedmedia.cms.pipeline.payload.PipelineKeys;
import com.unifiedmedia.cms.pipeline.spi.PipelineEventListener;
import com.unifiedmedia.cms.repository.TaskNodeLogRepository;
import com.unifiedmedia.cms.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 任务日志管理器 — 所有日志写操作使用 {@code REQUIRES_NEW} 独立事务，
 * 确保即使主流程回滚，日志也能落库，前端不会变成"瞎子"。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskLogManager {

    private final TaskRepository taskRepository;
    private final TaskNodeLogRepository taskNodeLogRepository;
    private final List<PipelineEventListener> eventListeners;

    /** 写启动日志 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveStartLog(UUID taskId, UUID jobId, PipelineTaskContext ctx, String message) {
        String fp = ctx.getPipelineData(PipelineKeys.ABSOLUTE_PATH, String.class);
        Task t = findOrCreateTask(jobId, fp);
        t.setAssetId(ctx.getAssetId());
        t.setStatus("RUNNING");
        taskRepository.save(t);

        String logOutput = "1   系统前置检查\n    [OK] " + message;
        TaskNodeLog nodeLog = TaskNodeLog.builder()
                .taskId(t.getId()).nodeName("PreCheck").nodeLabel("系统前置检查")
                .status("SUCCESS").startTime(java.time.LocalDateTime.now()).endTime(java.time.LocalDateTime.now())
                .durationMs(0L).logOutput(logOutput).build();
        taskNodeLogRepository.save(nodeLog);

        var logEvent = new NodeLogEvent(t.getId(), jobId, nodeLog.getId(),
                "PreCheck", "系统前置检查", "SUCCESS",
                nodeLog.getStartTime().toString(), nodeLog.getEndTime().toString(),
                0L, logOutput, null);
        for (var listener : eventListeners) {
            listener.onNodeLogCreated(t.getId(), jobId, logEvent);
        }
    }

    /** 写节点执行结果日志 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveNodeResults(UUID jobId, PipelineTaskContext ctx, List<NodeExecutionResult> results) {
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
        var last = results.get(results.size() - 1);
        if ("SUCCESS".equals(last.status())) t.setLastCompletedNode(last.nodeName());
        t.setStatus(results.stream().anyMatch(r -> "FAILED".equals(r.status())) ? "FAILED" : "SUCCESS");
        taskRepository.save(t);
    }

    /** 写最终状态 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFinalStatus(UUID jobId, PipelineTaskContext ctx) {
        String fp = ctx.getPipelineData(PipelineKeys.ABSOLUTE_PATH, String.class);
        if (fp == null) return;
        Task t = findOrCreateTask(jobId, fp);
        t.setAssetId(ctx.getAssetId());
        t.setStatus("SUCCESS");
        taskRepository.save(t);
    }

    /** 写失败日志 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailLog(UUID taskId, String errorMessage) {
        // Task 已在 startLog 中创建，这里仅记录错误
        log.error("[TaskLogManager] Task {} failed: {}", taskId, errorMessage);
    }

    private Task findOrCreateTask(UUID jobId, String filePath) {
        List<Task> tasks = taskRepository.findByJobIdOrderByCreatedAtAsc(jobId);
        for (int i = tasks.size() - 1; i >= 0; i--) {
            if (filePath.equals(tasks.get(i).getFilePath())) return tasks.get(i);
        }
        return Task.builder().jobId(jobId).filePath(filePath).build();
    }
}
