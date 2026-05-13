package com.unifiedmedia.cms.pipeline.appliers;

import com.unifiedmedia.cms.entity.BookDetail;
import com.unifiedmedia.cms.entity.MediaDetail;
import com.unifiedmedia.cms.pipeline.payload.ScrapeCandidate;
import com.unifiedmedia.cms.pipeline.spi.CandidateApplier;

public class BookDetailApplier implements CandidateApplier {

    @Override
    public boolean supports(Class<? extends MediaDetail> detailType) {
        return BookDetail.class.isAssignableFrom(detailType);
    }

    @Override
    public void apply(MediaDetail detail, ScrapeCandidate candidate) {
        BookDetail book = (BookDetail) detail;
        if (candidate.extraData() != null) {
            book.updatePublisher(candidate.extraData().get("publisher"));
        }
    }
}
