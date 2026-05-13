package com.unifiedmedia.cms.pipeline.core;

import java.util.List;

/**
 * 管道节点统一接口 — 所有节点的契约
 * <p>
 * 节点分为四种类型：
 * <ul>
 *   <li>INPUT  — 发现数据源，创建任务</li>
 *   <li>PROCESSING — 转换/提取/丰富数据</li>
 *   <li>OUTPUT — 持久化结果（写文件+入库）</li>
 *   <li>FLOW_CONTROL — 流程控制（条件分支、延时等）</li>
 * </ul>
 */
public interface PipelineNode {

    // ---- 元数据（后端自描述，前端动态渲染） ----

    /** 唯一标识，如 "FileSnifferNode" */
    String getNodeName();

    /** 展示名称，如 "文件嗅探" */
    String getNodeLabel();

    /** 图标（emoji），如 "🔍" */
    String getNodeIcon();

    /** 功能描述 */
    String getDescription();

    /** 节点类型 */
    NodeType getNodeType();

    /** 本节点需要的配置字段列表，前端据此渲染配置表单 */
    default List<ConfigFieldDef> getConfigSchema() {
        return List.of();
    }

    /** 配置项中需要在节点卡片上摘要显示的 key 列表 */
    default List<String> getUiSummaryKeys() {
        return List.of();
    }

    // ---- 执行契约 ----

    /** 引擎调度前调用：判断上下文是否满足本节点前置条件 */
    boolean canExecute(TaskContext context);

    /** 核心执行逻辑，直接修改 context；若需仲裁则抛出 ArbitrationRequiredException */
    void execute(TaskContext context) throws Exception;
}
