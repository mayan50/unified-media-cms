package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.AssetCreator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetCreatorRepository extends JpaRepository<AssetCreator, UUID> {
    List<AssetCreator> findByAssetId(UUID assetId);
    void deleteByAssetId(UUID assetId);
}
