package com.unifiedmedia.cms.pipeline.sync;

import com.unifiedmedia.cms.entity.Asset;
import com.unifiedmedia.cms.entity.Tag;
import com.unifiedmedia.cms.pipeline.core.AssetDraft;
import com.unifiedmedia.cms.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TagSynchronizer implements AssetDataSynchronizer {

    private final TagRepository tagRepository;

    @Override
    public void synchronize(AssetDraft draft, Asset managedAsset) {
        Set<String> pendingTags = draft.getPendingTagNames();
        if (pendingTags.isEmpty()) return;

        List<Tag> existing = tagRepository.findByNameIn(pendingTags);
        Map<String, Tag> existMap = existing.stream().collect(Collectors.toMap(Tag::getName, t -> t));

        for (String name : pendingTags) {
            Tag tag = existMap.getOrDefault(name, tagRepository.save(Tag.builder().name(name).build()));
            managedAsset.getTags().add(tag);
            if (!existMap.containsKey(name)) existMap.put(name, tag); // 缓存新建的，避免后续重复 save
        }

        draft.getPendingTagNames().clear();
    }
}
