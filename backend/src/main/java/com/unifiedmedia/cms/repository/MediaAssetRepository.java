package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
    List<MediaAsset> findByStatus(String status);
    List<MediaAsset> findByLibraryId(UUID libraryId);
    List<MediaAsset> findByLibraryIdAndStatus(UUID libraryId, String status);
}
