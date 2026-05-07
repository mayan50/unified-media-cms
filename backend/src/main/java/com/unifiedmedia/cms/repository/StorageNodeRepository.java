package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.StorageNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StorageNodeRepository extends JpaRepository<StorageNode, UUID> {
}
