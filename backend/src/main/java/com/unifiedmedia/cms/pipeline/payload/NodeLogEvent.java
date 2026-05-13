package com.unifiedmedia.cms.pipeline.payload;

import java.util.UUID;

/**
 * 节点日志事件载体 — 从 BatchJobService 抛给 PipelineEventListener，
 * 完全与 JPA Entity 解耦。
 */
public record NodeLogEvent(
    UUID taskId,
    UUID jobId,
    UUID id,
    String nodeName,
    String nodeLabel,
    String status,
    String startTime,
    String endTime,
    Long durationMs,
    String logOutput,
    String errorMessage
) {}
