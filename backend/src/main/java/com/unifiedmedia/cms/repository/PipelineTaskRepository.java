package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.PipelineTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PipelineTaskRepository extends JpaRepository<PipelineTask, UUID> {
    List<PipelineTask> findByCurrentStatus(String status);
    List<PipelineTask> findByAssetId(UUID assetId);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
}
