package com.unifiedmedia.cms.pipeline.spi;

import com.unifiedmedia.cms.entity.MediaDetail;

import java.util.UUID;

/**
 * 媒体详情加载器 — 引擎根据 mediaType 动态调用匹配的实现，
 * 从数据库加载历史 Detail 注入 TaskContext。
 */
public interface MediaDetailLoader {
    boolean supports(String mediaType);
    MediaDetail load(UUID assetId);
}
