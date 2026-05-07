package com.unifiedmedia.cms.dto;

import lombok.Data;

@Data
public class AssetUpdateRequest {
    private String title;
    private String summary;
    private Integer publishYear;
    private String coverUrl;
}
