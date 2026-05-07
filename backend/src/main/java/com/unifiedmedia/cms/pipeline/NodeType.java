package com.unifiedmedia.cms.pipeline;

import lombok.Getter;

/**
 * 节点类型 — 按数据流角色划分
 */
@Getter
public enum NodeType {

    INPUT("数据输入", "📥"),
    PROCESSING("数据处理", "⚙️"),
    OUTPUT("数据输出", "📤"),
    FLOW_CONTROL("流程控制", "🔀");

    private final String label;
    private final String icon;

    NodeType(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }
}
