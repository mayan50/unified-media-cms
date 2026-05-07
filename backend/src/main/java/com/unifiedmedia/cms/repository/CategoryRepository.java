package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByNameAndMediaType(String name, String mediaType);
}
