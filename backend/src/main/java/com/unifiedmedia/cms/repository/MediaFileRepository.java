package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {
    List<MediaFile> findByAssetId(UUID assetId);
}
