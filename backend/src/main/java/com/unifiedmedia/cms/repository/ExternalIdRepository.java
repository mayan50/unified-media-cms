package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.ExternalId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExternalIdRepository extends JpaRepository<ExternalId, UUID> {
    List<ExternalId> findByAssetId(UUID assetId);
    void deleteByAssetId(UUID assetId);
}
