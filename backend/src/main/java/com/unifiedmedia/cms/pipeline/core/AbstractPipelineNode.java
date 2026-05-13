package com.unifiedmedia.cms.pipeline.core;

import java.util.List;

/**
 * 管道节点抽象基类 — 提供元数据的默认存储
 * <p>
 * 子类通过构造函数注入元数据，只需实现 canExecute 和 execute。
 * 四种类型的子类：{@link BaseInputNode}、{@link BaseProcessingNode}、
 * {@link BaseOutputNode}、{@link BaseFlowControlNode}
 */
public abstract class AbstractPipelineNode implements PipelineNode {

    private final String nodeName;
    private final String nodeLabel;
    private final String nodeIcon;
    private final String description;
    private final NodeType nodeType;
    private final List<ConfigFieldDef> configSchema;
    private final List<String> uiSummaryKeys;

    protected AbstractPipelineNode(String nodeName, String nodeLabel, String nodeIcon,
                                   String description, NodeType nodeType,
                                   List<ConfigFieldDef> configSchema, List<String> uiSummaryKeys) {
        this.nodeName = nodeName;
        this.nodeLabel = nodeLabel;
        this.nodeIcon = nodeIcon;
        this.description = description;
        this.nodeType = nodeType;
        this.configSchema = configSchema != null ? configSchema : List.of();
        this.uiSummaryKeys = uiSummaryKeys != null ? uiSummaryKeys : List.of();
    }

    @Override
    public String getNodeName() { return nodeName; }

    @Override
    public String getNodeLabel() { return nodeLabel; }

    @Override
    public String getNodeIcon() { return nodeIcon; }

    @Override
    public String getDescription() { return description; }

    @Override
    public NodeType getNodeType() { return nodeType; }

    @Override
    public List<ConfigFieldDef> getConfigSchema() { return configSchema; }

    @Override
    public List<String> getUiSummaryKeys() { return uiSummaryKeys; }
}
