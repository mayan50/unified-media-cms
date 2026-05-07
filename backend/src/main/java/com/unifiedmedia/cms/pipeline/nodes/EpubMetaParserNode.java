package com.unifiedmedia.cms.pipeline.nodes;

import com.unifiedmedia.cms.pipeline.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
public class EpubMetaParserNode extends BaseProcessingNode {

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
        return (context.getDetectedMimeType() != null && context.getDetectedMimeType().contains("epub"))
                || "EPUB".equals(context.getDetectedFormat());
    }

    @Override
    public void execute(TaskContext context) throws IOException {
        String path = context.getAbsolutePath();
        if (path == null) path = context.getRelativePath();
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
        context.setEpubMetadata(metadata);
        if (metadata.containsKey("title")) context.setAiTitle((String) metadata.get("title"));
        if (metadata.containsKey("creator")) context.setAiAuthor((String) metadata.get("creator"));
        if (metadata.containsKey("description")) context.setAiSummary((String) metadata.get("description"));
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
