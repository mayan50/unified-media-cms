package com.unifiedmedia.cms.pipeline.nodes.control;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class RenameNode extends BaseProcessingNode {

    public RenameNode() {
        super("RenameNode", "重命名", "✏️",
                "根据元数据自动生成规范文件名（作者 - 标题）",
                List.of(),
                List.of());
    }

    @Override
    public boolean canExecute(TaskContext context) {
        String title = context.getAsset() != null ? context.getAsset().getTitle() : null;
        return title != null && !title.isBlank();
    }

    @Override
    public void execute(TaskContext context) {
        String title = context.getAsset() != null ? context.getAsset().getTitle() : null;
        String author = context.getPipelineData(PipelineKeys.AUTHORS, String.class);
        String newName = (author != null ? author + " - " : "") + title;
        context.setPipelineData(PipelineKeys.RENAMED_TITLE, newName);
        context.addLog("ok", "重命名: " + newName);
        log.info("[RenameNode] Renamed to: {}", newName);
    }
}
