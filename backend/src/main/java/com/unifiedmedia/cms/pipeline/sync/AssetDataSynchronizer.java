package com.unifiedmedia.cms.pipeline.sync;

import com.unifiedmedia.cms.entity.Asset;
import com.unifiedmedia.cms.pipeline.core.AssetDraft;

/**
 * 资产维度同步器 — 将 Draft 中的 pending 意图转为数据库持久化记录。
 * <p>
 * 每种多对多关联维度各有一个实现，由 {@code AssetSyncService} 统一调度。
 */
public interface AssetDataSynchronizer {

    /** 从 draft 读取 pending 集合，执行查重后合并到 managedAsset */
    void synchronize(AssetDraft draft, Asset managedAsset);
}
