package com.unifiedmedia.cms.pipeline.sync;

import com.unifiedmedia.cms.entity.Asset;
import com.unifiedmedia.cms.entity.Category;
import com.unifiedmedia.cms.pipeline.core.AssetDraft;
import com.unifiedmedia.cms.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CategorySynchronizer implements AssetDataSynchronizer {

    private final CategoryRepository categoryRepository;

    @Override
    public void synchronize(AssetDraft draft, Asset managedAsset) {
        Set<String> pendingCats = draft.getPendingCategoryNames();
        if (pendingCats.isEmpty()) return;

        List<Category> existing = categoryRepository.findByNameIn(pendingCats);
        Map<String, Category> existMap = existing.stream().collect(Collectors.toMap(Category::getName, c -> c));

        for (String name : pendingCats) {
            Category cat = existMap.getOrDefault(name,
                    categoryRepository.save(Category.builder().name(name).mediaType(managedAsset.getMediaType()).build()));
            managedAsset.getCategories().add(cat);
            if (!existMap.containsKey(name)) existMap.put(name, cat);
        }

        draft.getPendingCategoryNames().clear();
    }
}
