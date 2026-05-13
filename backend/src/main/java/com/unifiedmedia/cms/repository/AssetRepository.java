package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
    List<Asset> findByMediaType(String mediaType);

    @Query("SELECT COUNT(a) > 0 FROM Asset a JOIN a.tags t WHERE t.id = :tagId")
    boolean existsByTagId(@Param("tagId") UUID tagId);

    @Query("SELECT COUNT(a) > 0 FROM Asset a JOIN a.categories c WHERE c.id = :categoryId")
    boolean existsByCategoryId(@Param("categoryId") UUID categoryId);
}
