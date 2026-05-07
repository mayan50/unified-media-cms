package com.unifiedmedia.cms.pipeline.nodes;

import com.unifiedmedia.cms.pipeline.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RenameNode extends BaseProcessingNode {

    public RenameNode() {
        super("RenameNode", "重命名", "✏️",
                "根据元数据自动生成规范文件名（作者 - 标题）",
                List.of(),
                List.of());
    }

    @Override
    public boolean canExecute(TaskContext context) {
        return context.getFinalTitle() != null;
    }

    @Override
    public void execute(TaskContext context) {
        String title = context.getFinalTitle();
        String author = context.getFinalAuthor();
        String newName = (author != null ? author + " - " : "") + title;
        context.put("renamedTitle", newName);
        log.info("[RenameNode] Renamed to: {}", newName);
    }
}
