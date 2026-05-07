package com.unifiedmedia.cms.pipeline;

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
}
