package com.unifiedmedia.cms.pipeline.loaders;

import com.unifiedmedia.cms.entity.BookDetail;
import com.unifiedmedia.cms.entity.MediaDetail;
import com.unifiedmedia.cms.pipeline.spi.MediaDetailLoader;
import com.unifiedmedia.cms.repository.BookDetailRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class BookDetailLoader implements MediaDetailLoader {

    private final BookDetailRepository bookDetailRepository;

    @Override
    public boolean supports(String mediaType) {
        return "BOOK".equals(mediaType);
    }

    @Override
    public MediaDetail load(UUID assetId) {
        return bookDetailRepository.findByAssetId(assetId).orElse(null);
    }
}
