package com.unifiedmedia.cms.pipeline.core;

import java.util.List;

/**
 * 管道节点统一接口 — 仅保留执行契约与动态配置结构。
 * <p>
 * 静态元数据（名称、图标、类型等）统一通过 {@link com.unifiedmedia.cms.pipeline.core.annotation.NodeDef} 注解声明，
 * 引擎通过反射读取，无需实例化节点 Bean。
 * <p>
 * 旧版 getter 保留为 default 方法，回退到 {@code AbstractPipelineNode} 的存储值。
 */
public interface PipelineNode {

    /** 引擎调度前调用：判断上下文是否满足本节点前置条件 */
    boolean canExecute(TaskContext context);

    /** 核心执行逻辑，直接修改 context；若需仲裁则抛出 ArbitrationRequiredException */
    void execute(TaskContext context) throws Exception;

    /** 本节点需要的配置字段列表，前端据此渲染配置表单（动态结构，可运行时计算） */
    default List<ConfigFieldDef> getConfigSchema() { return List.of(); }

    /** 配置项中需要在节点卡片上摘要显示的 key 列表 */
    default List<String> getUiSummaryKeys() { return List.of(); }

    // ── 旧版元数据 getter（过渡期保留，优先从 @NodeDef 读取）──

    default String getNodeName() { return getClass().getSimpleName(); }
    default String getNodeLabel() { return getNodeName(); }
    default String getNodeIcon() { return ""; }
    default String getDescription() { return ""; }
    default NodeType getNodeType() { return NodeType.PROCESSING; }
}
