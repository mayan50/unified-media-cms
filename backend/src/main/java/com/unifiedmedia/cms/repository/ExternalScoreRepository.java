package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.ExternalScore;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExternalScoreRepository extends JpaRepository<ExternalScore, UUID> {
    List<ExternalScore> findByAssetId(UUID assetId);
}
