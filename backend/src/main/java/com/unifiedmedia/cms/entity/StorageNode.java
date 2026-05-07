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
@Table(name = "storage_nodes")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageNode {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "provider_type", nullable = false, length = 50)
    private String providerType; // LOCAL, MINIO, S3

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "connection_config", nullable = false, columnDefinition = "jsonb")
    private String connectionConfig;

    @Column(name = "is_readonly")
    @Builder.Default
    private Boolean isReadonly = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @JsonProperty("connectionConfig")
    public Object getConnectionConfigParsed() {
        if (connectionConfig == null) return null;
        try { return MAPPER.readValue(connectionConfig, new TypeReference<Object>() {}); }
        catch (Exception e) { return connectionConfig; }
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
