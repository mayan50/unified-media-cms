package com.unifiedmedia.cms.pipeline.sync;

import com.unifiedmedia.cms.entity.Asset;
import com.unifiedmedia.cms.pipeline.core.AssetDraft;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 资产同步调度中心 — 自动发现所有 {@link AssetDataSynchronizer} 实现并遍历分发。
 * <p>
 * 调用方必须在事务内调用此方法（{@code MANDATORY}）。
 */
@Service
@RequiredArgsConstructor
public class AssetSyncService {

    private final List<AssetDataSynchronizer> synchronizes;

    @Transactional(propagation = Propagation.MANDATORY)
    public void syncPendingData(AssetDraft draft, Asset managedAsset) {
        for (AssetDataSynchronizer sync : synchronizes) {
            sync.synchronize(draft, managedAsset);
        }
    }
}
