package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.Creator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreatorRepository extends JpaRepository<Creator, UUID> {
    Optional<Creator> findByName(String name);
}
