package com.unifiedmedia.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "book_details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDetail implements MediaDetail {

    @Id
    private UUID id;

    @Column(name = "asset_id", unique = true)
    private UUID assetId;

    private String subtitle;
    private String publisher;

    @Column(name = "published_date", columnDefinition = "DATE")
    private LocalDate publishedDate;

    private String language;

    private Integer pages;

    @Column(name = "series_name")
    private String seriesName;

    @Column(name = "series_number")
    private BigDecimal seriesNumber;

    @Column(name = "total_books")
    private Integer totalBooks;

    @Column(name = "word_count")
    private Long wordCount;

    @Column(name = "chapter_count")
    private Integer chapterCount;

    @Column(name = "completion_status", length = 20)
    private String completionStatus;

    @Column(columnDefinition = "DECIMAL(3,1)")
    private BigDecimal rating;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "locked_fields", columnDefinition = "jsonb")
    @Builder.Default
    private Set<String> lockedFields = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_data", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> extraData = new HashMap<>();

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ==================== Smart Setters ====================

    public void updateSubtitle(String v)           { if (v != null && !isLocked("subtitle")) this.subtitle = v; }
    public void updatePublisher(String v)          { if (v != null && !isLocked("publisher")) this.publisher = v; }
    public void updateLanguage(String v)           { if (v != null && !isLocked("language")) this.language = v; }
    public void updateCompletionStatus(String v)   { if (v != null && !isLocked("completionStatus")) this.completionStatus = v; }
    public void updateSeriesName(String v)         { if (v != null && !isLocked("seriesName")) this.seriesName = v; }

    public void updatePages(Integer v)             { if (v != null && !isLocked("pages")) this.pages = v; }
    public void updateWordCount(Long v)            { if (v != null && !isLocked("wordCount")) this.wordCount = v; }
    public void updateChapterCount(Integer v)      { if (v != null && !isLocked("chapterCount")) this.chapterCount = v; }
    public void updateSeriesNumber(BigDecimal v)   { if (v != null && !isLocked("seriesNumber")) this.seriesNumber = v; }
    public void updateTotalBooks(Integer v)        { if (v != null && !isLocked("totalBooks")) this.totalBooks = v; }
    public void updateRating(BigDecimal v)         { if (v != null && !isLocked("rating")) this.rating = v; }
    public void updatePublishedDate(LocalDate v)   { if (v != null && !isLocked("publishedDate")) this.publishedDate = v; }

    public boolean isFieldLocked(String fieldName) {
        return lockedFields.contains(fieldName);
    }

    private boolean isLocked(String fieldName) {
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
