package com.unifiedmedia.cms.pipeline.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 顶层媒体业务大类 — 全站资产分类的真理之源。
 */
@Getter
@RequiredArgsConstructor
public enum MediaCategory {

    BOOK("图书", "📖"),
    VIDEO("视频", "🎥"),
    AUDIO("音频", "🎵"),
    IMAGE("图片", "🖼️"),
    DOCUMENT("文档", "📄"),
    ARCHIVE("压缩归档", "📦"),
    EXECUTABLE("可执行程序", "⚙️"),
    UNKNOWN("未知", "❓");

    private final String label;
    private final String icon;
}
