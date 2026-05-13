package com.unifiedmedia.cms.pipeline.mergers;

import com.unifiedmedia.cms.entity.Asset;
import com.unifiedmedia.cms.entity.Tag;
import com.unifiedmedia.cms.pipeline.core.TaskContext;
import com.unifiedmedia.cms.pipeline.payload.PipelineKeys;
import com.unifiedmedia.cms.pipeline.spi.RelationMerger;
import com.unifiedmedia.cms.repository.TagRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class TagRelationMerger implements RelationMerger {

    private final TagRepository tagRepository;

    @Override
    public void merge(TaskContext context, Asset asset) {
        if (asset.isFieldLocked("tags")) return;

        List<String> tags = context.getPipelineList(PipelineKeys.TAGS, String.class);
        if (tags == null || tags.isEmpty()) return;

        asset.getTags().clear();
        for (String tagName : tags) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
            asset.getTags().add(tag);
        }
        context.addLog("ok", "保存标签: " + String.join(", ", tags));
    }
}
