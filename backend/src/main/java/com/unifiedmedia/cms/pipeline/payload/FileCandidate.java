package com.unifiedmedia.cms.pipeline.payload;

/**
 * 文件候选 — FileSnifferNode 的产出，包含单文件的所有元数据。
 * 替代原来污染全局 baseCtx 的 absolutePath / detectedFormat 等字段。
 */
public record FileCandidate(
    String absolutePath,
    String relativePath,
    String mimeType,
    String format
) {}
