package com.unifiedmedia.cms.pipeline.mergers;

import com.unifiedmedia.cms.entity.Asset;
import com.unifiedmedia.cms.entity.AssetCreator;
import com.unifiedmedia.cms.entity.Creator;
import com.unifiedmedia.cms.pipeline.core.TaskContext;
import com.unifiedmedia.cms.pipeline.payload.PipelineKeys;
import com.unifiedmedia.cms.pipeline.spi.RelationMerger;
import com.unifiedmedia.cms.repository.AssetCreatorRepository;
import com.unifiedmedia.cms.repository.CreatorRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class CreatorRelationMerger implements RelationMerger {

    private final CreatorRepository creatorRepository;
    private final AssetCreatorRepository assetCreatorRepository;

    @Override
    public void merge(TaskContext context, Asset asset) {
        if (asset.isFieldLocked("authors")) return;

        List<String> authors = context.getPipelineList(PipelineKeys.AUTHORS, String.class);
        if (authors == null || authors.isEmpty()) return;

        for (String authorName : authors) {
            if (authorName == null || authorName.isBlank()) continue;
            Creator creator = creatorRepository.findByName(authorName)
                    .orElseGet(() -> creatorRepository.save(Creator.builder().name(authorName).build()));

            List<AssetCreator> existing = assetCreatorRepository.findByAssetId(asset.getId());
            boolean exists = existing.stream()
                    .anyMatch(ac -> ac.getCreatorId().equals(creator.getId()) && "作者".equals(ac.getRole()));
            if (!exists) {
                assetCreatorRepository.save(AssetCreator.builder()
                        .assetId(asset.getId()).creatorId(creator.getId()).role("作者").build());
            }
        }
        context.addLog("ok", "保存作者: " + String.join(", ", authors));
    }
}
