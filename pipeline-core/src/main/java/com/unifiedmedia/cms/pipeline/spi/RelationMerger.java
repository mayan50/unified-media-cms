package com.unifiedmedia.cms.pipeline.spi;

import com.unifiedmedia.cms.entity.Asset;
import com.unifiedmedia.cms.pipeline.core.TaskContext;

/**
 * 关系合并器 — 从 PipelineData 中提取意图数据并合并到 Asset 的关联集合中。
 * <p>
 * 每种多对多关联（Tag、Creator、未来的 Category 等）各有一个实现，
 * ArchiveNode 通过 Spring 注入 List&lt;RelationMerger&gt; 遍历调用，自身不感知具体合并逻辑。
 */
public interface RelationMerger {

    /** 从上下文中提取意图数据，安全地合并到 Asset 实体中 */
    void merge(TaskContext context, Asset asset);
}
