package com.unifiedmedia.cms.pipeline.core;

import java.util.List;
import java.util.UUID;

/**
 * 流水线节点执行上下文 — 纯数据总线接口。
 * <p>
 * 节点通过此接口获取系统元数据、读写临时数据、操作资产草稿及收集日志。
 * 严禁在此接口中出现任何 JPA {@code @Entity} 类型。
 */
public interface TaskContext {

    // ── 系统元数据 ──

    UUID getTaskId();
    UUID getAssetId();

    // ── 草稿访问 ──

    AssetDraft getAssetDraft();
    void setAssetDraft(AssetDraft draft);

    MediaDetailDraft getDetailDraft();
    void setDetailDraft(MediaDetailDraft draft);

    // ── 临时数据总线（仅放入库的计算废料）──

    <T> T getPipelineData(String key, Class<T> type);
    void setPipelineData(String key, Object value);

    @SuppressWarnings("unchecked")
    <T> List<T> getPipelineList(String key, Class<T> elementType);

    default boolean hasPipelineData(String key) {
        return getPipelineData(key, Object.class) != null;
    }

    // ── 日志收集 ──

    void addLog(String level, String message);
    List<String> drainLogs();

    // ── 错误收集 ──

    void addError(String message);
    List<String> getErrors();
}
