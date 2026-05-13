package com.unifiedmedia.cms.pipeline.spi;

import com.unifiedmedia.cms.entity.MediaDetail;
import com.unifiedmedia.cms.pipeline.payload.ScrapeCandidate;

/**
 * 候选者应用器 — 将 ScrapeCandidate 中的数据安全赋值给特定的 MediaDetail。
 */
public interface CandidateApplier {
    boolean supports(Class<? extends MediaDetail> detailType);
    void apply(MediaDetail detail, ScrapeCandidate candidate);
}
