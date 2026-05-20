package com.unifiedmedia.cms.pipeline.core;

import java.util.List;

/**
 * 管道节点抽象基类 — 过渡期保留构造函数传参方式。
 * <p>
 * 静态元数据优先从 {@link com.unifiedmedia.cms.pipeline.core.annotation.NodeDef} 注解读取，
 * 此基类的 getter 方法作为回退（旧节点未标注注解时使用）。
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

    public String getNodeName() { return nodeName; }
    public String getNodeLabel() { return nodeLabel; }
    public String getNodeIcon() { return nodeIcon; }
    public String getDescription() { return description; }
    public NodeType getNodeType() { return nodeType; }

    @Override
    public List<ConfigFieldDef> getConfigSchema() { return configSchema; }

    @Override
    public List<String> getUiSummaryKeys() { return uiSummaryKeys; }
}
