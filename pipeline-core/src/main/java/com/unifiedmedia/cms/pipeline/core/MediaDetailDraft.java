package com.unifiedmedia.cms.pipeline.core;

/**
 * 媒体详情草稿标记接口 — 多态锚点。
 * <p>
 * 当前实现：{@link BookDetailDraft}。未来扩展：ComicDetailDraft, VideoDetailDraft。
 * 与 backend 的 {@code MediaDetail} 接口语义对等但物理隔离，严禁与 JPA 实体发生继承关系。
 */
public interface MediaDetailDraft {
}
