package com.unifiedmedia.cms.pipeline.loaders;

import com.unifiedmedia.cms.entity.MediaDetail;
import com.unifiedmedia.cms.pipeline.spi.MediaDetailLoader;
import com.unifiedmedia.cms.repository.BookDetailRepository;

import java.util.UUID;

public record BookDetailLoader(BookDetailRepository bookDetailRepository) implements MediaDetailLoader {

    @Override
    public boolean supports(String mediaType) {
        return "BOOK".equals(mediaType);
    }

    @Override
    public MediaDetail load(UUID assetId) {
        return bookDetailRepository.findByAssetId(assetId).orElse(null);
    }
}
