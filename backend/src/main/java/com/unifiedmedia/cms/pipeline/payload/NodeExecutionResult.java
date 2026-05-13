package com.unifiedmedia.cms.pipeline.payload;

import java.util.List;

/**
 * 单节点执行结果 — PipelineEngine 每个节点执行后产出，FileTaskExecutor 批量落库。
 */
public record NodeExecutionResult(
    String nodeName,
    String nodeLabel,
    String status,
    long durationMs,
    List<String> logs,
    String errorMessage
) {}
