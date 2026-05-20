package com.unifiedmedia.cms.pipeline.nodes.processing;
import com.unifiedmedia.cms.pipeline.core.annotation.NodeDef;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@NodeDef(name = "VideoFrameNode", label = "视频抽帧", icon = "🎬", type = NodeType.PROCESSING)
public class VideoFrameNode extends BaseProcessingNode {

    public VideoFrameNode() {
        super("VideoFrameNode", "视频抽帧", "🖼",
                "从视频文件中提取关键帧截图（需 FFmpeg）",
                List.of(),
                List.of());
    }

    @Override
    public boolean canExecute(TaskContext context) {
        String mime = context.getPipelineData(PipelineKeys.DETECTED_MIME_TYPE, String.class);
        return mime != null && mime.startsWith("video/");
    }

    @Override
    public void execute(TaskContext context) {
        String relativePath = context.getPipelineData(PipelineKeys.RELATIVE_PATH, String.class);
        log.info("[VideoFrameNode] Extracting frames from: {}", relativePath);
        // TODO: FFmpeg integration
        context.setPipelineData(PipelineKeys.FRAME_COUNT, 0);
        context.addLog("warn", "视频抽帧: " + relativePath + " (FFmpeg待集成)");
    }
}
