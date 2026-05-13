package com.unifiedmedia.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "assets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "media_type", nullable = false, length = 50)
    private String mediaType;

    @Column(name = "publish_year")
    private Integer publishYear;

    @Column(name = "cover_url", length = 2000)
    private String coverUrl;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "asset_tags",
            joinColumns = @JoinColumn(name = "asset_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "asset_categories",
            joinColumns = @JoinColumn(name = "asset_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    @Builder.Default
    private Set<Category> categories = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "locked_fields", columnDefinition = "jsonb")
    @Builder.Default
    private Set<String> lockedFields = new HashSet<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Smart Setters (field-lock aware) ====================

    public void updateTitle(String v) {
        if (v != null && !lockedFields.contains("title")) this.title = v;
    }

    public void updateSummary(String v) {
        if (v != null && !lockedFields.contains("summary")) this.summary = v;
    }

    public void updateCoverUrl(String v) {
        if (v != null && !lockedFields.contains("coverUrl")) this.coverUrl = v;
    }

    public void updatePublishYear(Integer v) {
        if (v != null && !lockedFields.contains("publishYear")) this.publishYear = v;
    }

    public boolean isFieldLocked(String fieldName) {
        return lockedFields.contains(fieldName);
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
