package com.unifiedmedia.cms.pipeline.core;

import com.unifiedmedia.cms.entity.Asset;
import com.unifiedmedia.cms.entity.MediaDetail;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 节点执行上下文 — 节点通过此接口获取资产数据、管道中间数据和日志能力。
 * <p>
 * 节点不应持有 Repository 或其他基础设施依赖，只操作此上下文中的领域对象。
 */
public interface TaskContext {

    UUID getTaskId();
    Asset getAsset();

    /** 获取媒体详情（可能为 null，节点应使用 getOrCreateDetail） */
    <T extends MediaDetail> T getDetail(Class<T> type);

    /** 按需获取或创建媒体详情，factory 负责构建新实例 */
    <T extends MediaDetail> T getOrCreateDetail(Class<T> type, Supplier<T> factory);

    // ── 管道中间数据 ──

    /** Type-safe scalar access (replaces old getPipelineData without Class param) */
    <T> T getPipelineData(String key, Class<T> type);
    void setPipelineData(String key, Object value);

    /** Type-safe list access (replaces all @SuppressWarnings casts) */
    <T> List<T> getPipelineList(String key, Class<T> elementType);

    // ── 日志 ──

    void addLog(String level, String message);
    List<String> drainLogs();

    /** Backward compat (keep hasPipelineData) */
    default boolean hasPipelineData(String key) {
        return getPipelineData(key, Object.class) != null;
    }
}
