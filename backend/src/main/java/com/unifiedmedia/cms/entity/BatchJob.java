package com.unifiedmedia.cms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_graph", nullable = false, columnDefinition = "jsonb")
    private String executionGraph;

    @Column(name = "input_storage_node_id")
    private UUID inputStorageNodeId;

    @JsonIgnore
    @Column(name = "input_path")
    private String inputPathText;

    @Column(name = "output_storage_node_id")
    private UUID outputStorageNodeId;

    @JsonIgnore
    @Column(name = "output_path")
    private String outputPathText;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("executionGraph")
    public Object getExecutionGraphParsed() {
        if (executionGraph == null) return null;
        try { return MAPPER.readValue(executionGraph, new TypeReference<Object>() {}); }
        catch (Exception e) { return executionGraph; }
    }

    @JsonProperty("inputPath")
    public Object getInputPath() {
        if (inputStorageNodeId == null && inputPathText == null) return null;
        return Map.of("storage_node_id", inputStorageNodeId != null ? inputStorageNodeId.toString() : "",
                      "path", inputPathText != null ? inputPathText : "");
    }

    @JsonProperty("outputPath")
    public Object getOutputPath() {
        if (outputStorageNodeId == null && outputPathText == null) return null;
        return Map.of("storage_node_id", outputStorageNodeId != null ? outputStorageNodeId.toString() : "",
                      "path", outputPathText != null ? outputPathText : "");
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
