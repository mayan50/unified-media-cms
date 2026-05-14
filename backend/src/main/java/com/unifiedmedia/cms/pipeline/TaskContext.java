package com.unifiedmedia.cms.pipeline;

import com.unifiedmedia.cms.entity.Asset;
import com.unifiedmedia.cms.entity.MediaDetail;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.function.Supplier;

/**
 * 流式上下文 - 在节点间流转的载体
 */
@Data
@Accessors(chain = true)
public class TaskContext implements com.unifiedmedia.cms.pipeline.core.TaskContext {

    private UUID taskId;
    private UUID assetId;

    // 任务入口参数
    private UUID storageNodeId;
    private String relativePath;
    private String absolutePath;
    private String sourceDirectory;            // Pipeline 入口：整个任务的输入根目录

    // 文件探测结果
    private String detectedMimeType;
    private String detectedFormat; // TXT, EPUB

    // 嗅探结果：目录下所有文件及其 MIME 类型
    private Map<String, String> taggedFiles = new LinkedHashMap<>();  // path → mimeType

    // 提取结果
    private String extractedText;
    private Map<String, Object> epubMetadata;

    // AI 分析结果
    private String aiTitle;
    private String aiAuthor;
    private List<String> aiTags;
    private String aiSummary;

    // 刮削结果
    private List<Map<String, Object>> scraperCandidates; // 多个候选结果
    private Map<String, Object> selectedResult;          // 仲裁后选择的结果

    // 最终合并的元数据
    private String finalTitle;
    private String finalAuthor;
    private List<String> finalTags;
    private String finalSummary;
    private String finalCoverUrl;
    private Integer finalPublishYear;

    // 转换结果
    private byte[] convertedEpubBytes;

    // 归档结果
    private UUID archivedFileId;
    private UUID archivedStorageNodeId;

    // 错误收集
    private List<String> errors = new ArrayList<>();

    // 通用数据存储 - 节点间传递任意数据
    private final Map<String, Object> data = new HashMap<>();

    // ── 日志 ──
    private final List<String> logs = new ArrayList<>();

    public TaskContext put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    // ── core.TaskContext bridge ──

    @Override
    public Asset getAsset() { return null; }

    @Override
    public <T extends MediaDetail> T getDetail(Class<T> type) { return null; }

    @Override
    public <T extends MediaDetail> T getOrCreateDetail(Class<T> type, Supplier<T> factory) {
        return factory.get();
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
            if (elementType.isInstance(item)) result.add((T) item);
        }
        return result;
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
}
