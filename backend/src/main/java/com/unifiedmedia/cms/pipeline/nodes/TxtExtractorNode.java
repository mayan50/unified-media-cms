package com.unifiedmedia.cms.pipeline.nodes;

import com.unifiedmedia.cms.pipeline.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
public class TxtExtractorNode extends BaseProcessingNode {

    private static final int MAX_CHARS = 3000;

    public TxtExtractorNode() {
        super("TxtExtractorNode", "TXT 采样", "📄",
                "从纯文本文件中采样提取内容片段，供后续节点分析",
                List.of(),
                List.of());
    }

    @Override
    public boolean canExecute(TaskContext context) {
        return "text/plain".equals(context.getDetectedMimeType())
                || "TXT".equals(context.getDetectedFormat());
    }

    @Override
    public void execute(TaskContext context) throws IOException {
        String path = context.getAbsolutePath();
        if (path == null) path = context.getRelativePath();
        String content = Files.readString(Path.of(path), StandardCharsets.UTF_8);
        String extracted = content.length() > MAX_CHARS
                ? content.substring(0, MAX_CHARS) : content;
        context.setExtractedText(extracted);
        log.info("[TxtExtractorNode] Extracted {} chars from: {}", extracted.length(), path);
    }
}
