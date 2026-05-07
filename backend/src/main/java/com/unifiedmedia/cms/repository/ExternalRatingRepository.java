package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.ExternalRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExternalRatingRepository extends JpaRepository<ExternalRating, UUID> {
    List<ExternalRating> findByAssetId(UUID assetId);
}
