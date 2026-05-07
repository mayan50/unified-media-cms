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
import java.util.UUID;

@Data
@Entity
@Table(name = "pipeline_task_logs")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineTaskLog {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "node_name", nullable = false, length = 100)
    private String nodeName;

    @Column(nullable = false, length = 50)
    private String status; // SUCCESS, SKIPPED, FAILED

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_payload", columnDefinition = "jsonb")
    private String outputPayload;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @JsonProperty("outputPayload")
    public Object getOutputPayloadParsed() {
        if (outputPayload == null) return null;
        try { return MAPPER.readValue(outputPayload, new TypeReference<Object>() {}); }
        catch (Exception e) { return outputPayload; }
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
