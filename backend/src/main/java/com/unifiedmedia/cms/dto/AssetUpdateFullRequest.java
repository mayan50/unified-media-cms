package com.unifiedmedia.cms.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AssetUpdateFullRequest {
    // ── Asset fields ──
    private String title;
    private String summary;
    private String coverUrl;
    private String mediaType;

    // ── BookDetail fields ──
    private String subtitle;
    private String publisher;
    private String publishedDate;
    private String language;
    private Integer pages;
    private Long wordCount;
    private Integer chapterCount;
    private String seriesName;
    private BigDecimal seriesNumber;
    private Integer totalBooks;
    private String completionStatus;
    private BigDecimal rating;

    // ── Relations ──
    private List<String> tagIds;
    private List<String> categoryIds;
    private List<CreatorRef> creators;
    private List<ExternalIdRef> externalIds;

    // ── Locked fields (JSON string from frontend) ──
    private String lockedFields;

    @Data
    public static class CreatorRef {
        private String name;
        private String role;
    }

    @Data
    public static class ExternalIdRef {
        private String source;
        private String identifier;
    }
}
