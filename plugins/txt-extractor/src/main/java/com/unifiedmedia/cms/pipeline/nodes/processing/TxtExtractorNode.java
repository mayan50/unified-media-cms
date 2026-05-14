package com.unifiedmedia.cms.pipeline.nodes.processing;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Slf4j
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
        return "text/plain".equals(context.getPipelineData(PipelineKeys.DETECTED_MIME_TYPE, String.class))
                || "TXT".equals(context.getPipelineData(PipelineKeys.DETECTED_FORMAT, String.class));
    }

    @Override
    public void execute(TaskContext context) throws IOException {
        String path = context.getPipelineData(PipelineKeys.ABSOLUTE_PATH, String.class);
        if (path == null) path = context.getPipelineData(PipelineKeys.RELATIVE_PATH, String.class);
        String content = FileUtil.readString(Path.of(path));
        String extracted = content.length() > MAX_CHARS
                ? content.substring(0, MAX_CHARS) : content;
        context.setPipelineData(PipelineKeys.EXTRACTED_TEXT, extracted);
        context.addLog("ok", "文本采样: 提取 " + extracted.length() + " 字符");
        log.info("[TxtExtractorNode] Extracted {} chars from: {}", extracted.length(), path);
    }
}
