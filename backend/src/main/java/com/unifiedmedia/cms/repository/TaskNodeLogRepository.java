package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.TaskNodeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskNodeLogRepository extends JpaRepository<TaskNodeLog, UUID> {
    List<TaskNodeLog> findByTaskIdOrderByStartTimeAsc(UUID taskId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TaskNodeLog t WHERE t.taskId = :taskId")
    void deleteByTaskId(@Param("taskId") UUID taskId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TaskNodeLog l WHERE l.taskId IN :taskIds")
    void deleteByTaskIdIn(Collection<UUID> taskIds);
}
