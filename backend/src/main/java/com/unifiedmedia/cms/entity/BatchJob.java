package com.unifiedmedia.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "batch_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJob {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(length = 200)
    private String name;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_graph", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> executionGraph;

    @Column(name = "input_storage_node_id")
    private UUID inputStorageNodeId;

    @Column(name = "input_path")
    private String inputPathText;

    @Column(name = "output_storage_node_id")
    private UUID outputStorageNodeId;

    @Column(name = "output_path")
    private String outputPathText;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
