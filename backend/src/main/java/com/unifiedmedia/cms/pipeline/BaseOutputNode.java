package com.unifiedmedia.cms.pipeline;

import java.util.List;

/**
 * 输出节点基类 — 负责持久化结果（写文件、入库）
 */
public abstract class BaseOutputNode extends AbstractPipelineNode {

    protected BaseOutputNode(String nodeName, String nodeLabel, String nodeIcon,
                             String description, List<ConfigFieldDef> configSchema,
                             List<String> uiSummaryKeys) {
        super(nodeName, nodeLabel, nodeIcon, description, NodeType.OUTPUT, configSchema, uiSummaryKeys);
    }
}
