package com.unifiedmedia.cms.pipeline;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.*;

/**
 * 流式上下文 - 在节点间流转的载体
 */
@Data
@Accessors(chain = true)
public class TaskContext {

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
}
