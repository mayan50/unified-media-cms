package com.unifiedmedia.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "book_details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "asset_id", unique = true)
    private UUID assetId;

    private String subtitle;
    private String publisher;

    @Column(name = "published_date")
    private String publishedDate;

    private String language;

    @Column(name = "isbn_10")
    private String isbn10;

    @Column(name = "isbn_13")
    private String isbn13;

    private String asin;
    private Integer pages;

    @Column(name = "series_name")
    private String seriesName;

    @Column(name = "series_number")
    private java.math.BigDecimal seriesNumber;

    @Column(name = "total_books")
    private Integer totalBooks;

    @Column(name = "douban_id")
    private String doubanId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_data", columnDefinition = "jsonb DEFAULT '{}'::jsonb")
    private String extraData;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = java.time.LocalDateTime.now();
        if (extraData == null) extraData = "{}";
    }
}
