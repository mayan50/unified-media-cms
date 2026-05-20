package com.unifiedmedia.cms.pipeline.core;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 图书详情草稿 — 纯内存 POJO，与 {@code BookDetail} 实体字段一一对应。
 * <p>
 * 仅包含当前 BookDetail 实体已有的业务字段，不含 id/assetId/createdAt/updatedAt/lockedFields/extraData
 * 等系统管理字段。后续实体字段扩展时同步增加。
 */
@Data
public class BookDetailDraft implements MediaDetailDraft {

    private String subtitle;
    private String publisher;
    /** 出版日期，格式为 ISO 日期字符串（如 "2026-05"），合并时由 Mapper 解析为 LocalDate */
    private String publishedDate;
    private String language;
    private Integer pages;
    private Long wordCount;
    private Integer chapterCount;
    private String seriesName;
    private BigDecimal seriesNumber;
    private Integer totalBooks;
    /** 连载状态：ongoing / completed */
    private String completionStatus;
    /** 评分，Decimal(3,1) */
    private BigDecimal rating;
}
