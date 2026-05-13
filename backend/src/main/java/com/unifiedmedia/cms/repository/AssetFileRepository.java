package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.AssetFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface AssetFileRepository extends JpaRepository<AssetFile, UUID> {
    List<AssetFile> findByAssetId(UUID assetId);

    @Transactional
    void deleteByAssetId(UUID assetId);
}
