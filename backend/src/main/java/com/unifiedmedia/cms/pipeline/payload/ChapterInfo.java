package com.unifiedmedia.cms.pipeline.payload;

/**
 * 章节信息 — 管道中间载体，替代旧的 Map<String,Object> 章节目录。
 */
public record ChapterInfo(String title, int lineNumber, int charOffset, String format) {}
