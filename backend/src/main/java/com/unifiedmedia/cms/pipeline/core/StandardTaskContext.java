package com.unifiedmedia.cms.pipeline.core;

import com.unifiedmedia.cms.entity.Asset;
import com.unifiedmedia.cms.entity.MediaDetail;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * TaskContext 默认实现 — 双层数据模型：
 * <ul>
 *   <li>globalData — 作业级全局只读配置（targetPath 等），构造时传入，不可修改</li>
 *   <li>localData  — 单文件私有读写黑板，所有 set 操作只影响 localData</li>
 * </ul>
 * 读取时降级：先查 local，未命中查 global。彻底消除跨文件数据污染。
 */
public class StandardTaskContext implements TaskContext {

    private final UUID taskId;
    private final Asset asset;
    private MediaDetail detail;
    private final Map<String, Object> globalData;
    private final Map<String, Object> localData = new ConcurrentHashMap<>();
    private final List<String> logs = new ArrayList<>();

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
    public Asset getAsset() { return asset; }

    @Override
    public <T extends MediaDetail> T getDetail(Class<T> type) {
        if (type.isInstance(detail)) return type.cast(detail);
        return null;
    }

    @Override
    public <T extends MediaDetail> T getOrCreateDetail(Class<T> type, Supplier<T> factory) {
        if (type.isInstance(detail)) return type.cast(detail);
        T newDetail = factory.get();
        this.detail = newDetail;
        return newDetail;
    }

    // ── 双层数据访问 ──

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

    // ── 日志 ──

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
}
