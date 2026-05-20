package com.unifiedmedia.cms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifiedmedia.cms.dto.AssetUpdateFullRequest;
import com.unifiedmedia.cms.entity.*;
import com.unifiedmedia.cms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetFileRepository assetFileRepository;
    private final StorageNodeRepository storageNodeRepository;
    private final BookDetailRepository bookDetailRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;
    private final AssetCreatorRepository assetCreatorRepository;
    private final CreatorRepository creatorRepository;
    private final ExternalIdRepository externalIdRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Transactional(readOnly = true)
    public Optional<Asset> getAsset(UUID id) {
        return assetRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<AssetFile> getAssetFiles(UUID assetId) {
        return assetFileRepository.findByAssetId(assetId);
    }

    @Transactional
    public Asset updateAsset(UUID id, Map<String, Object> body) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + id));
        if (body.get("title") != null) asset.setTitle((String) body.get("title"));
        if (body.get("summary") != null) asset.setSummary((String) body.get("summary"));
        if (body.get("publishYear") != null) asset.setPublishYear((Integer) body.get("publishYear"));
        if (body.get("coverUrl") != null) asset.setCoverUrl((String) body.get("coverUrl"));
        return assetRepository.save(asset);
    }

    @Transactional
    public void deleteAsset(UUID id) {
        assetCreatorRepository.deleteByAssetId(id);
        bookDetailRepository.findByAssetId(id).ifPresent(bookDetailRepository::delete);
        externalIdRepository.deleteByAssetId(id);
        assetFileRepository.deleteByAssetId(id);
        assetRepository.deleteById(id);
        log.info("[AssetService] Deleted asset {} and related data", id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAssetDetail(UUID id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + id));

        List<AssetFile> files = assetFileRepository.findByAssetId(id);

        List<Map<String, Object>> fileList = new ArrayList<>();
        for (AssetFile file : files) {
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("id", file.getId());
            fileInfo.put("fileFormat", file.getFileFormat());
            fileInfo.put("relativePath", file.getRelativePath());
            fileInfo.put("fileSize", file.getFileSize());
            fileInfo.put("isPrimary", file.getIsPrimary());

            storageNodeRepository.findById(file.getStorageNodeId()).ifPresent(node -> {
                Map<String, Object> storageInfo = new HashMap<>();
                storageInfo.put("id", node.getId());
                storageInfo.put("name", node.getName());
                storageInfo.put("providerType", node.getProviderType());
                fileInfo.put("storageNode", storageInfo);
            });

            fileList.add(fileInfo);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("asset", asset);
        result.put("files", fileList);
        return result;
    }

    @Transactional
    public Map<String, Object> updateAssetFull(UUID assetId, AssetUpdateFullRequest req) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));

        if (req.getTitle() != null) asset.setTitle(req.getTitle());
        if (req.getSummary() != null) asset.setSummary(req.getSummary());
        if (req.getCoverUrl() != null) asset.setCoverUrl(req.getCoverUrl());
        if (req.getMediaType() != null) asset.setMediaType(req.getMediaType());
        assetRepository.save(asset);

        BookDetail bd = bookDetailRepository.findByAssetId(assetId)
                .orElseGet(() -> BookDetail.builder().assetId(assetId).build());
        bd.setLockedFields(parseLockedFields(req.getLockedFields()));

        bd.updateSubtitle(req.getSubtitle());
        bd.updatePublisher(req.getPublisher());
        bd.updateLanguage(req.getLanguage());
        bd.updateCompletionStatus(req.getCompletionStatus());
        bd.updateSeriesName(req.getSeriesName());
        bd.updatePages(req.getPages());
        bd.updateWordCount(req.getWordCount());
        bd.updateChapterCount(req.getChapterCount());
        bd.updateSeriesNumber(req.getSeriesNumber());
        bd.updateTotalBooks(req.getTotalBooks());
        bd.updateRating(req.getRating());
        if (req.getPublishedDate() != null && !req.getPublishedDate().isBlank()) {
            try { bd.updatePublishedDate(LocalDate.parse(req.getPublishedDate() + "-01")); } catch (Exception ignored) {}
        }

        bookDetailRepository.save(bd);

        if (req.getTagIds() != null) {
            asset.getTags().clear();
            for (String tagId : req.getTagIds()) {
                try {
                    UUID id = UUID.fromString(tagId);
                    tagRepository.findById(id).ifPresent(t -> asset.getTags().add(t));
                } catch (IllegalArgumentException ignored) {}
            }
            assetRepository.save(asset);
        }

        if (req.getCategoryIds() != null) {
            asset.getCategories().clear();
            for (String catId : req.getCategoryIds()) {
                try {
                    UUID id = UUID.fromString(catId);
                    categoryRepository.findById(id).ifPresent(c -> asset.getCategories().add(c));
                } catch (IllegalArgumentException ignored) {}
            }
            assetRepository.save(asset);
        }

        if (req.getCreators() != null) {
            assetCreatorRepository.deleteByAssetId(assetId);
            assetCreatorRepository.flush();
            for (var ref : req.getCreators()) {
                if (ref.getName() == null || ref.getName().isBlank()) continue;
                Creator creator = creatorRepository.findByName(ref.getName())
                        .orElseGet(() -> creatorRepository.save(Creator.builder().name(ref.getName()).build()));
                AssetCreator ac = AssetCreator.builder()
                        .assetId(assetId).creatorId(creator.getId()).role(ref.getRole() != null ? ref.getRole() : "作者")
                        .build();
                assetCreatorRepository.save(ac);
            }
        }

        if (req.getExternalIds() != null) {
            externalIdRepository.deleteByAssetId(assetId);
            for (var ref : req.getExternalIds()) {
                if (ref.getSource() == null || ref.getIdentifier() == null) continue;
                ExternalId ext = ExternalId.builder()
                        .assetId(assetId).source(ref.getSource()).identifier(ref.getIdentifier())
                        .build();
                externalIdRepository.save(ext);
            }
        }

        log.info("[AssetService] Full update completed for asset {}", assetId);
        return Map.of("success", true);
    }

    private Set<String> parseLockedFields(String lockedFieldsJson) {
        if (lockedFieldsJson == null || lockedFieldsJson.isBlank()) return Set.of();
        try {
            return new HashSet<>(MAPPER.readValue(lockedFieldsJson, new TypeReference<List<String>>() {}));
        } catch (Exception e) {
            log.warn("Failed to parse lockedFields: {}", lockedFieldsJson);
            return Set.of();
        }
    }
}
