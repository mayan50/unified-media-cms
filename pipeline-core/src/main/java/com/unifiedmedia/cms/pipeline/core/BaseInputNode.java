package com.unifiedmedia.cms.pipeline.core;

import java.util.List;

/**
 * 输入节点基类 — 负责发现数据源、创建任务
 */
public abstract class BaseInputNode extends AbstractPipelineNode {

    protected BaseInputNode(String nodeName, String nodeLabel, String nodeIcon,
                            String description, List<ConfigFieldDef> configSchema,
                            List<String> uiSummaryKeys) {
        super(nodeName, nodeLabel, nodeIcon, description, NodeType.INPUT, configSchema, uiSummaryKeys);
    }
}
