package com.unifiedmedia.cms.pipeline.core;

import com.unifiedmedia.cms.entity.Asset;
import com.unifiedmedia.cms.entity.MediaDetail;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * TaskContext 默认实现 — 双层数据模型。
 * <p>
 * 旧节点兼容：保留 {@link #getAsset()} 真实返回 JPA 实体。
 * 新节点应使用 {@link PipelineTaskContext} + {@link AssetDraft}。
 */
public class StandardTaskContext implements TaskContext {

    private final UUID taskId;
    private final Asset asset;
    private MediaDetail detail;
    private final Map<String, Object> globalData;
    private final Map<String, Object> localData = new ConcurrentHashMap<>();
    private final List<String> logs = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private AssetDraft assetDraft;
    private MediaDetailDraft detailDraft;

    public StandardTaskContext(UUID taskId, Asset asset) {
        this(taskId, asset, null, Map.of());
    }

    public StandardTaskContext(UUID taskId, Asset asset, MediaDetail detail) {
        this(taskId, asset, detail, Map.of());
    }

    public StandardTaskContext(UUID taskId, Asset asset, MediaDetail detail, Map<String, Object> globalData) {
        this.taskId = taskId;
        this.asset = asset;
        this.detail = detail;
        this.globalData = globalData != null ? Map.copyOf(globalData) : Map.of();
    }

    @Override
    public UUID getTaskId() { return taskId; }

    @Override
    public UUID getAssetId() { return asset != null ? asset.getId() : null; }

    public Asset getAsset() { return asset; }

    public <T extends MediaDetail> T getDetail(Class<T> type) {
        if (type.isInstance(detail)) return type.cast(detail);
        return null;
    }

    public <T extends MediaDetail> T getOrCreateDetail(Class<T> type, Supplier<T> factory) {
        if (type.isInstance(detail)) return type.cast(detail);
        T newDetail = factory.get();
        this.detail = newDetail;
        return newDetail;
    }

    @Override
    public AssetDraft getAssetDraft() { return assetDraft; }

    @Override
    public void setAssetDraft(AssetDraft draft) { this.assetDraft = draft; }

    @Override
    public MediaDetailDraft getDetailDraft() { return detailDraft; }

    @Override
    public void setDetailDraft(MediaDetailDraft draft) { this.detailDraft = draft; }

    @Override
    public <T> T getPipelineData(String key, Class<T> type) {
        Object val = localData.get(key);
        if (type.isInstance(val)) return type.cast(val);
        val = globalData.get(key);
        if (type.isInstance(val)) return type.cast(val);
        return null;
    }

    @Override
    public <T> List<T> getPipelineList(String key, Class<T> elementType) {
        Object raw = localData.get(key) != null ? localData.get(key) : globalData.get(key);
        if (!(raw instanceof Iterable<?> iterable)) return new ArrayList<>();
        List<T> result = new ArrayList<>();
        for (Object item : iterable) {
            if (elementType.isInstance(item)) result.add(elementType.cast(item));
        }
        return result;
    }

    @Override
    public void setPipelineData(String key, Object value) {
        if (value != null) localData.put(key, value);
        else localData.remove(key);
    }

    @Override
    public void addLog(String level, String message) {
        String prefix = switch (level) {
            case "ok" -> "[OK] "; case "warn" -> "[WARN] "; case "err" -> "[ERR] "; default -> "";
        };
        logs.add(prefix + message);
    }

    @Override
    public List<String> drainLogs() {
        List<String> copy = new ArrayList<>(logs); logs.clear(); return copy;
    }

    @Override
    public void addError(String message) { errors.add(message); }

    @Override
    public List<String> getErrors() { return errors; }
}
