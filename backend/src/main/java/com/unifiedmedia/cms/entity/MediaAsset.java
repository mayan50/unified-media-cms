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
@Table(name = "media_assets")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAsset {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "library_id", nullable = false)
    private UUID libraryId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "media_type", nullable = false, length = 50)
    private String mediaType; // V1: BOOK

    @Column(name = "publish_year")
    private Integer publishYear;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column
    private String summary;

    @Column(nullable = false, length = 50)
    private String status; // PROCESSING, PENDING_MANUAL, COMPLETED

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tech_specs", columnDefinition = "jsonb")
    @Builder.Default
    private String techSpecs = "{}";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @JsonProperty("techSpecs")
    public Object getTechSpecsParsed() {
        if (techSpecs == null) return null;
        try { return MAPPER.readValue(techSpecs, new TypeReference<Object>() {}); }
        catch (Exception e) { return techSpecs; }
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
