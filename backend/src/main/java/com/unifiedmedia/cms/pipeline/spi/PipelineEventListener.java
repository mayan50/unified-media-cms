package com.unifiedmedia.cms.pipeline.spi;

import com.unifiedmedia.cms.pipeline.payload.NodeLogEvent;

import java.util.UUID;

/**
 * 管线事件监听器 — 引擎在关键节点遍历所有实现，推送状态变更和日志追加。
 * 实现类（如 WebSocket 推送）放在外围基础设施包中，核心引擎不依赖任何通信协议。
 */
public interface PipelineEventListener {

    void onNodeLogCreated(UUID taskId, UUID jobId, NodeLogEvent logEvent);

    void onTaskStatusChanged(UUID taskId, UUID jobId, String status, String errorMessage);

    void onJobStatusChanged(UUID jobId, String status);

    /** 领域事件：任务的历史日志在数据库中被清空 */
    default void onNodeLogsCleared(UUID taskId, UUID jobId) {}
}
