package com.unifiedmedia.cms.pipeline.mapper;

import com.unifiedmedia.cms.entity.BookDetail;
import com.unifiedmedia.cms.entity.MediaDetail;
import com.unifiedmedia.cms.pipeline.core.BookDetailDraft;
import com.unifiedmedia.cms.pipeline.core.MediaDetailDraft;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 媒体详情草稿 <-> 实体 双向映射器。
 * <p>
 * 通过 {@code instanceof} 向下转型处理多态详情映射。
 * 新增媒介类型时在此加分支即可。
 */
@Component
public class MediaDetailDraftMapper {

    /**
     * 阶段 1：将 Draft 合并到受管态实体（保护 Smart Setter）。
     */
    public void mergeDraftToEntity(MediaDetailDraft draft, MediaDetail managedDetail) {
        if (draft instanceof BookDetailDraft bookDraft && managedDetail instanceof BookDetail bookEntity) {
            if (bookDraft.getSubtitle() != null) bookEntity.updateSubtitle(bookDraft.getSubtitle());
            if (bookDraft.getPublisher() != null) bookEntity.updatePublisher(bookDraft.getPublisher());
            if (bookDraft.getLanguage() != null) bookEntity.updateLanguage(bookDraft.getLanguage());
            if (bookDraft.getCompletionStatus() != null) bookEntity.updateCompletionStatus(bookDraft.getCompletionStatus());
            if (bookDraft.getSeriesName() != null) bookEntity.updateSeriesName(bookDraft.getSeriesName());
            if (bookDraft.getPages() != null) bookEntity.updatePages(bookDraft.getPages());
            if (bookDraft.getWordCount() != null) bookEntity.updateWordCount(bookDraft.getWordCount());
            if (bookDraft.getChapterCount() != null) bookEntity.updateChapterCount(bookDraft.getChapterCount());
            if (bookDraft.getSeriesNumber() != null) bookEntity.updateSeriesNumber(bookDraft.getSeriesNumber());
            if (bookDraft.getTotalBooks() != null) bookEntity.updateTotalBooks(bookDraft.getTotalBooks());
            if (bookDraft.getRating() != null) bookEntity.updateRating(bookDraft.getRating());
            if (bookDraft.getPublishedDate() != null && !bookDraft.getPublishedDate().isBlank()) {
                try { bookEntity.updatePublishedDate(LocalDate.parse(bookDraft.getPublishedDate() + "-01")); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 阶段 2：将实体最新状态拍成快照回写 Draft。
     */
    public MediaDetailDraft createDraftFromEntity(MediaDetail entity) {
        if (entity instanceof BookDetail bookEntity) {
            BookDetailDraft draft = new BookDetailDraft();
            draft.setSubtitle(bookEntity.getSubtitle());
            draft.setPublisher(bookEntity.getPublisher());
            draft.setPublishedDate(bookEntity.getPublishedDate() != null ? bookEntity.getPublishedDate().toString().substring(0, 7) : null);
            draft.setLanguage(bookEntity.getLanguage());
            draft.setPages(bookEntity.getPages());
            draft.setWordCount(bookEntity.getWordCount());
            draft.setChapterCount(bookEntity.getChapterCount());
            draft.setSeriesName(bookEntity.getSeriesName());
            draft.setSeriesNumber(bookEntity.getSeriesNumber());
            draft.setTotalBooks(bookEntity.getTotalBooks());
            draft.setCompletionStatus(bookEntity.getCompletionStatus());
            draft.setRating(bookEntity.getRating());
            return draft;
        }
        return null;
    }
}
