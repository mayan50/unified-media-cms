package com.unifiedmedia.cms.pipeline.payload;

/**
 * 管道键值契约 — 节点间通过 TaskContext.setPipelineData / getPipelineData 传递数据时使用的键。
 * <p>
 * 集中管理消除魔法字符串，IDE 可追踪读写关系。
 */
public final class PipelineKeys {
    private PipelineKeys() {}

    // ── 路径与存储 ──
    public static final String ABSOLUTE_PATH = "absolutePath";
    public static final String RELATIVE_PATH = "relativePath";
    public static final String SOURCE_DIRECTORY = "sourceDirectory";
    public static final String STORAGE_NODE_ID = "storageNodeId";
    public static final String TARGET_PATH = "targetPath";
    public static final String TEMP_SANDBOX_PATH = "tempSandboxPath";

    // ── 文件探测 ──
    public static final String DETECTED_FORMAT = "detectedFormat";
    public static final String DETECTED_MIME_TYPE = "detectedMimeType";
    public static final String TAGGED_FILES = "taggedFiles";
    public static final String FILE_CANDIDATES = "fileCandidates";

    // ── 中间物料 ──
    public static final String EXTRACTED_TEXT = "extractedText";
    public static final String EPUB_METADATA = "epubMetadata";
    public static final String CONVERTED_EPUB = "convertedEpubBytes";
    public static final String CHAPTERS = "chapters";
    public static final String CHAPTER_COUNT = "chapterCount";
    public static final String CHAPTER_TITLES = "chapterTitles";
    public static final String SCRAPER_CANDIDATES = "scraperCandidates";
    public static final String SELECTED_RESULT = "selectedResult";
    public static final String RENAMED_TITLE = "renamedTitle";
    public static final String FRAME_COUNT = "frameCount";
    public static final String CUSTOM_PATTERN = "customPattern";
    public static final String DELAY_MS = "delayMs";
    public static final String AI_SUGGESTION = "aiSuggestion";

    // ── 流程控制 ──
    public static final String ROUTER_CONDITION = "routerCondition";
    public static final String ROUTER_BRANCH = "routerBranch";

    // ── 关联集合意图（ArchiveNode 收口，由 RelationMerger 实现类消费）──
    public static final String AUTHORS = "authors";
    public static final String TAGS = "tags";
}
