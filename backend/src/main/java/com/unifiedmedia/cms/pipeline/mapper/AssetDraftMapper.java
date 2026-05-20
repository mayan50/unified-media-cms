package com.unifiedmedia.cms.pipeline.mapper;

import com.unifiedmedia.cms.entity.Asset;
import com.unifiedmedia.cms.entity.Tag;
import com.unifiedmedia.cms.entity.Category;
import com.unifiedmedia.cms.pipeline.core.AssetDraft;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资产草稿 <-> 实体 双向映射器。
 * <p>
 * 严禁使用 BeanUtils 等反射工具，必须显式调用实体上的 Smart Setter 方法
 * 以激活字段锁检查逻辑。
 */
@Component
public class AssetDraftMapper {

    /**
     * 阶段 1：将 Draft 的基础属性安全合并到受管态实体。
     * 必须调用 {@code updateXxx()} 触发 lockedFields 保护。
     */
    public void mergeDraftToEntity(AssetDraft draft, Asset managedAsset) {
        if (draft.getTitle() != null) managedAsset.updateTitle(draft.getTitle());
        if (draft.getSummary() != null) managedAsset.updateSummary(draft.getSummary());
        if (draft.getCoverUrl() != null) managedAsset.updateCoverUrl(draft.getCoverUrl());
        if (draft.getPublishYear() != null) managedAsset.updatePublishYear(draft.getPublishYear());
        if (draft.getMediaType() != null) managedAsset.setMediaType(draft.getMediaType());
    }

    /**
     * 阶段 2：将实体最新状态拍成快照回写 Draft。
     * pending 集合保持空（意图已在 sync 中终结），current 集合填充实体已有数据供下游只读。
     */
    public AssetDraft createDraftFromEntity(Asset entity) {
        AssetDraft draft = new AssetDraft();
        draft.setTitle(entity.getTitle());
        draft.setSummary(entity.getSummary());
        draft.setCoverUrl(entity.getCoverUrl());
        draft.setPublishYear(entity.getPublishYear());
        draft.setMediaType(entity.getMediaType());
        draft.setLockedFields(entity.getLockedFields());

        // 已持久化标签 → current，供下游只读
        if (entity.getTags() != null) {
            Set<String> tagNames = entity.getTags().stream()
                    .map(Tag::getName)
                    .collect(Collectors.toSet());
            draft.setCurrentTags(tagNames);
        }

        // 已持久化分类 → current
        if (entity.getCategories() != null) {
            Set<String> catNames = entity.getCategories().stream()
                    .map(Category::getName)
                    .collect(Collectors.toSet());
            draft.setCurrentCategories(catNames);
        }

        return draft;
    }
}
