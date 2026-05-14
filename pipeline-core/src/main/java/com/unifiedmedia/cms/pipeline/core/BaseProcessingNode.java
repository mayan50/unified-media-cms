package com.unifiedmedia.cms.pipeline.core;

import java.util.List;

/**
 * 处理节点基类 — 负责数据转换、提取、分析、刮削等
 */
public abstract class BaseProcessingNode extends AbstractPipelineNode {

    protected BaseProcessingNode(String nodeName, String nodeLabel, String nodeIcon,
                                 String description, List<ConfigFieldDef> configSchema,
                                 List<String> uiSummaryKeys) {
        super(nodeName, nodeLabel, nodeIcon, description, NodeType.PROCESSING, configSchema, uiSummaryKeys);
    }

    /**
     * 仅在目标字段未被锁定时写入 PipelineData。
     * 用于刮削节点产出集合意图（authors/tags），由 ArchiveNode 的 RelationMerger 消费。
     */
    protected void setIntentIfUnlocked(TaskContext ctx, String fieldName, String pipelineKey, Object value) {
        if (!ctx.getAsset().isFieldLocked(fieldName)) {
            ctx.setPipelineData(pipelineKey, value);
        }
    }
}
