package com.unifiedmedia.cms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Entity
@Table(name = "pipeline_tasks")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineTask {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "asset_id")
    private UUID assetId;

    @Column(name = "current_status", nullable = false, length = 50)
    private String currentStatus; // QUEUED, RUNNING, PENDING_MANUAL, FAILED, COMPLETED

    @Column(name = "template_id")
    private UUID templateId;

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_graph", columnDefinition = "jsonb")
    private String executionGraph;

    @Column(name = "stuck_node_id", length = 100)
    private String stuckNodeId;

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "task_context", columnDefinition = "jsonb")
    private String taskContext;

    @Column(name = "input_storage_node_id")
    private UUID inputStorageNodeId;

    @Column(name = "input_path_text")
    private String inputPathText;

    @Column(name = "output_storage_node_id")
    private UUID outputStorageNodeId;

    @Column(name = "output_path_text")
    private String outputPathText;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("inputPath")
    public Object getInputPathObj() {
        if (inputStorageNodeId == null && inputPathText == null) return null;
        return Map.of("storage_node_id", inputStorageNodeId != null ? inputStorageNodeId.toString() : "",
                      "path", inputPathText != null ? inputPathText : "");
    }

    @JsonProperty("outputPath")
    public Object getOutputPathObj() {
        if (outputStorageNodeId == null && outputPathText == null) return null;
        return Map.of("storage_node_id", outputStorageNodeId != null ? outputStorageNodeId.toString() : "",
                      "path", outputPathText != null ? outputPathText : "");
    }

    @JsonProperty("executionGraph")
    public Object getExecutionGraphParsed() {
        if (executionGraph == null) return null;
        try { return MAPPER.readValue(executionGraph, new TypeReference<Object>() {}); }
        catch (Exception e) { return executionGraph; }
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
