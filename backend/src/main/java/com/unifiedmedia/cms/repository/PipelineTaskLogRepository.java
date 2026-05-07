package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.PipelineTaskLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PipelineTaskLogRepository extends JpaRepository<PipelineTaskLog, UUID> {
    List<PipelineTaskLog> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
}
