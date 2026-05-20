package com.unifiedmedia.cms.pipeline.nodes.parser;

import com.unifiedmedia.cms.pipeline.core.annotation.NodeDef;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@NodeDef(name = "EpubMetaParserNode", label = "EPUB 解析", icon = "📖", type = NodeType.PROCESSING)
public class EpubMetaParserNode extends BaseProcessingNode implements EntityDataOperator {

    private static final Pattern TITLE_PATTERN = Pattern.compile("<dc:title[^>]*>([^<]+)</dc:title>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATOR_PATTERN = Pattern.compile("<dc:creator[^>]*>([^<]+)</dc:creator>", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN = Pattern.compile("<dc:date[^>]*>([^<]+)</dc:date>", Pattern.CASE_INSENSITIVE);
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("<dc:description[^>]*>([^<]+)</dc:description>", Pattern.CASE_INSENSITIVE);

    public EpubMetaParserNode() {
        super("EpubMetaParserNode", "EPUB 解析", "📖",
                "解析 EPUB 文件的元数据（标题、作者、出版信息）",
                List.of(),
                List.of());
    }

    @Override
    public boolean canExecute(TaskContext context) {
        String mime = context.getPipelineData(PipelineKeys.DETECTED_MIME_TYPE, String.class);
        return (mime != null && mime.contains("epub"))
                || "EPUB".equals(context.getPipelineData(PipelineKeys.DETECTED_FORMAT, String.class));
    }

    @Override
    public void execute(TaskContext context) throws IOException {
        String path = context.getPipelineData(PipelineKeys.ABSOLUTE_PATH, String.class);
        if (path == null) path = context.getPipelineData(PipelineKeys.RELATIVE_PATH, String.class);
        Map<String, Object> metadata = new HashMap<>();
        try (var zis = new ZipInputStream(Files.newInputStream(Path.of(path)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".opf")) {
                    String opfContent = new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    metadata.putAll(parseOpf(opfContent));
                    break;
                }
                zis.closeEntry();
            }
        }
        context.setPipelineData(PipelineKeys.EPUB_METADATA, metadata);
        if (metadata.containsKey("title")) context.getAssetDraft().setTitle((String) metadata.get("title"));
        if (metadata.containsKey("creator")) {
            Object creator = metadata.get("creator");
            if (creator instanceof String s && !s.isBlank()) {
                List<String> authors = creator instanceof List<?> l ? l.stream().map(Object::toString).toList()
                        : List.of(s);
                setIntentIfUnlocked(context, "authors", PipelineKeys.AUTHORS, authors);
            }
        }
        if (metadata.containsKey("description")) context.getAssetDraft().setSummary((String) metadata.get("description"));
        context.addLog("ok", "EPUB元数据: 标题=" + metadata.getOrDefault("title", "无")
                + ", 作者=" + metadata.getOrDefault("creator", "无"));
        log.info("[EpubMetaParserNode] Parsed metadata: {}", metadata);
    }

    private Map<String, Object> parseOpf(String opfContent) {
        Map<String, Object> result = new HashMap<>();
        matchSingle(TITLE_PATTERN, opfContent).ifPresent(v -> result.put("title", v));
        matchSingle(CREATOR_PATTERN, opfContent).ifPresent(v -> result.put("creator", v));
        matchSingle(DATE_PATTERN, opfContent).ifPresent(v -> result.put("date", v));
        matchSingle(DESCRIPTION_PATTERN, opfContent).ifPresent(v -> result.put("description", v));
        return result;
    }

    private Optional<String> matchSingle(Pattern pattern, String input) {
        Matcher m = pattern.matcher(input);
        return m.find() ? Optional.of(m.group(1).trim()) : Optional.empty();
    }
}
