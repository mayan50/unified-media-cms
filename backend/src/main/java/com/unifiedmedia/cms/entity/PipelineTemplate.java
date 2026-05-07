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
@Table(name = "pipeline_templates")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineTemplate {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column
    private String description;

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "graph_payload", columnDefinition = "jsonb")
    private String graphPayload;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @JsonProperty("graphPayload")
    public Object getGraphPayloadParsed() {
        if (graphPayload == null) return null;
        try { return MAPPER.readValue(graphPayload, new TypeReference<Object>() {}); }
        catch (Exception e) { return graphPayload; }
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
