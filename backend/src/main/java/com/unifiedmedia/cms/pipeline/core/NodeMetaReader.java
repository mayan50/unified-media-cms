package com.unifiedmedia.cms.pipeline.core;

import com.unifiedmedia.cms.pipeline.core.annotation.NodeDef;

/**
 * 节点元数据读取工具 — 优先从 @NodeDef 注解读取，回退到接口 getter。
 */
public final class NodeMetaReader {

    private NodeMetaReader() {}

    public static String getNodeName(PipelineNode node) {
        NodeDef def = node.getClass().getAnnotation(NodeDef.class);
        if (def != null) return def.name();
        return node.getNodeName();
    }

    public static String getNodeLabel(PipelineNode node) {
        NodeDef def = node.getClass().getAnnotation(NodeDef.class);
        if (def != null && !def.label().isEmpty()) return def.label();
        return node.getNodeLabel();
    }

    public static String getNodeIcon(PipelineNode node) {
        NodeDef def = node.getClass().getAnnotation(NodeDef.class);
        if (def != null && !def.icon().isEmpty()) return def.icon();
        return node.getNodeIcon();
    }

    public static String getDescription(PipelineNode node) {
        NodeDef def = node.getClass().getAnnotation(NodeDef.class);
        if (def != null && !def.description().isEmpty()) return def.description();
        return node.getDescription();
    }

    public static NodeType getNodeType(PipelineNode node) {
        NodeDef def = node.getClass().getAnnotation(NodeDef.class);
        if (def != null) return def.type();
        return node.getNodeType();
    }
}
