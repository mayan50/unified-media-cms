package com.unifiedmedia.cms.pipeline.sync;

import com.unifiedmedia.cms.entity.Asset;
import com.unifiedmedia.cms.entity.AssetCreator;
import com.unifiedmedia.cms.entity.Creator;
import com.unifiedmedia.cms.pipeline.core.AssetDraft;
import com.unifiedmedia.cms.repository.AssetCreatorRepository;
import com.unifiedmedia.cms.repository.CreatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class CreatorSynchronizer implements AssetDataSynchronizer {

    private final CreatorRepository creatorRepository;
    private final AssetCreatorRepository assetCreatorRepository;

    @Override
    public void synchronize(AssetDraft draft, Asset managedAsset) {
        Map<String, List<String>> pendingCreators = draft.getPendingCreators();
        if (pendingCreators.isEmpty()) return;

        // 先收集所有名字批量查询已有 Creator
        Set<String> allNames = new HashSet<>();
        pendingCreators.values().forEach(allNames::addAll);
        List<Creator> existing = creatorRepository.findByNameIn(allNames);
        Map<String, Creator> existMap = new HashMap<>();
        for (Creator c : existing) existMap.put(c.getName(), c);

        UUID assetId = managedAsset.getId();

        pendingCreators.forEach((role, names) -> {
            for (String name : names) {
                Creator creator = existMap.computeIfAbsent(name,
                        k -> creatorRepository.save(Creator.builder().name(k).build()));
                if (!assetCreatorRepository.existsByAssetIdAndCreatorIdAndRole(assetId, creator.getId(), role)) {
                    AssetCreator ac = AssetCreator.builder()
                            .assetId(assetId).creatorId(creator.getId()).role(role)
                            .build();
                    assetCreatorRepository.save(ac);
                }
            }
        });

        draft.getPendingCreators().clear();
    }
}
