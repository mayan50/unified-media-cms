package com.unifiedmedia.cms.pipeline;

import java.util.List;

/**
 * 流程控制节点基类 — 负责条件分支、延时、循环等
 */
public abstract class BaseFlowControlNode extends AbstractPipelineNode {

    protected BaseFlowControlNode(String nodeName, String nodeLabel, String nodeIcon,
                                  String description, List<ConfigFieldDef> configSchema,
                                  List<String> uiSummaryKeys) {
        super(nodeName, nodeLabel, nodeIcon, description, NodeType.FLOW_CONTROL, configSchema, uiSummaryKeys);
    }
}
