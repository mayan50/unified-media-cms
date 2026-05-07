package com.unifiedmedia.cms.service;

import com.unifiedmedia.cms.entity.MediaAsset;
import com.unifiedmedia.cms.entity.MediaFile;
import com.unifiedmedia.cms.repository.MediaAssetRepository;
import com.unifiedmedia.cms.repository.MediaFileRepository;
import com.unifiedmedia.cms.repository.StorageNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final MediaAssetRepository assetRepository;
    private final MediaFileRepository fileRepository;
    private final StorageNodeRepository storageNodeRepository;

    @Transactional(readOnly = true)
    public Optional<MediaAsset> getAsset(UUID id) {
        return assetRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<MediaAsset> getCompletedAssets() {
        return assetRepository.findByStatus("COMPLETED");
    }

    @Transactional(readOnly = true)
    public List<MediaAsset> getAllAssets() {
        return assetRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<MediaFile> getAssetFiles(UUID assetId) {
        return fileRepository.findByAssetId(assetId);
    }

    @Transactional
    public MediaAsset updateAsset(UUID id, String title, String summary, Integer publishYear, String coverUrl) {
        MediaAsset asset = assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + id));
        if (title != null) asset.setTitle(title);
        if (summary != null) asset.setSummary(summary);
        if (publishYear != null) asset.setPublishYear(publishYear);
        if (coverUrl != null) asset.setCoverUrl(coverUrl);
        return assetRepository.save(asset);
    }

    /**
     * 获取资产详情，包含物理文件和存储节点信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAssetDetail(UUID id) {
        MediaAsset asset = assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + id));

        List<MediaFile> files = fileRepository.findByAssetId(id);

        List<Map<String, Object>> fileList = new ArrayList<>();
        for (MediaFile file : files) {
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
}
