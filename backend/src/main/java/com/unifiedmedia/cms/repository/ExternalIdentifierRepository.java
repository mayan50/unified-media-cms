package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.ExternalIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExternalIdentifierRepository extends JpaRepository<ExternalIdentifier, UUID> {
    List<ExternalIdentifier> findByAssetId(UUID assetId);
}
