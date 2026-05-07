package com.unifiedmedia.cms.pipeline.nodes;

import com.unifiedmedia.cms.pipeline.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class VideoFrameNode extends BaseProcessingNode {

    public VideoFrameNode() {
        super("VideoFrameNode", "视频抽帧", "🖼",
                "从视频文件中提取关键帧截图（需 FFmpeg）",
                List.of(),
                List.of());
    }

    @Override
    public boolean canExecute(TaskContext context) {
        String mime = context.getDetectedMimeType();
        return mime != null && mime.startsWith("video/");
    }

    @Override
    public void execute(TaskContext context) {
        log.info("[VideoFrameNode] Extracting frames from: {}", context.getRelativePath());
        // TODO: FFmpeg integration
        context.put("frameCount", 0);
    }
}
