package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.BatchJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BatchJobRepository extends JpaRepository<BatchJob, UUID> {
    List<BatchJob> findByStatus(String status);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
}
