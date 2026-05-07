package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.PipelineTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PipelineTemplateRepository extends JpaRepository<PipelineTemplate, UUID> {
    Optional<PipelineTemplate> findByName(String name);
    Optional<PipelineTemplate> findByIsDefaultTrue();
}
