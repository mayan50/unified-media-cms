package com.unifiedmedia.cms.pipeline.core;

/**
 * 实体数据操作标记接口。
 * <p>
 * 实现了此接口的 {@link PipelineNode}，其 {@code execute()} 执行完毕后，
 * 底层 {@code PipelineOrchestrator} 会自动将 {@link AssetDraft} 的修改合并
 * 并持久化到真实的数据库实体中。
 * <p>
 * 此接口没有任何方法，仅作为类型标记（Marker Interface）使用。
 */
public interface EntityDataOperator {
}
