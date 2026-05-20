package com.unifiedmedia.cms.pipeline.core;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link TaskContext} 的纯净实现 — 不含任何业务硬编码字段，不引用任何 JPA 实体。
 * <p>
 * <b>职责：</b>
 * <ul>
 *   <li>系统元数据：taskId / assetId</li>
 *   <li>草稿持有：{@link AssetDraft} / {@link MediaDetailDraft}</li>
 *   <li>临时数据总线：仅用于不入库的计算废料</li>
 *   <li>日志与错误收集</li>
 * </ul>
 */
public class PipelineTaskContext implements TaskContext {

    // ── 1. 系统元数据 ──
    @Getter @Setter private UUID taskId;
    @Getter @Setter private UUID assetId;

    // ── 2. 目标文件（流水线一等公民）──
    @Getter @Setter private VfsFile targetFile;

    // ── 3. 业务草稿（纯 POJO，非 JPA 实体）──
    @Getter @Setter private AssetDraft assetDraft;
    @Getter @Setter private MediaDetailDraft detailDraft;

    // ── 4. 错误与日志 ──
    @Getter private final List<String> errors = new ArrayList<>();
    private final List<String> logs = new ArrayList<>();

    // ── 4. 临时数据总线（仅用于不入库的中间计算结果）──
    private final Map<String, Object> data = new ConcurrentHashMap<>();

    // ── 构造器 ──

    public PipelineTaskContext() {}

    public PipelineTaskContext(UUID taskId, UUID assetId) {
        this.taskId = taskId;
        this.assetId = assetId;
        this.assetDraft = new AssetDraft();
    }

    // ── 数据总线 ──

    public PipelineTaskContext put(String key, Object value) {
        if (value != null) data.put(key, value);
        else data.remove(key);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getPipelineData(String key, Class<T> type) {
        Object val = data.get(key);
        if (type.isInstance(val)) return (T) val;
        return null;
    }

    @Override
    public void setPipelineData(String key, Object value) {
        put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getPipelineList(String key, Class<T> elementType) {
        Object raw = data.get(key);
        if (!(raw instanceof Iterable<?> iterable)) return new ArrayList<>();
        List<T> result = new ArrayList<>();
        for (Object item : iterable) {
            if (elementType.isInstance(item)) result.add(elementType.cast(item));
        }
        return result;
    }

    // ── 日志 ──

    @Override
    public void addLog(String level, String message) {
        String prefix = switch (level) {
            case "ok" -> "[OK] ";
            case "warn" -> "[WARN] ";
            case "err" -> "[ERR] ";
            default -> "";
        };
        logs.add(prefix + message);
    }

    @Override
    public List<String> drainLogs() {
        List<String> copy = new ArrayList<>(logs);
        logs.clear();
        return copy;
    }

    @Override
    public void addError(String message) {
        errors.add(message);
    }
}
