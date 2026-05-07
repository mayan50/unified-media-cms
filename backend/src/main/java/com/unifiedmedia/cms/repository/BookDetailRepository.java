package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.BookDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookDetailRepository extends JpaRepository<BookDetail, UUID> {
    Optional<BookDetail> findByAssetId(UUID assetId);
}
