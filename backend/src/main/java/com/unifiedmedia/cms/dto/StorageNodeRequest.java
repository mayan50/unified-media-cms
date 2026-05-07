package com.unifiedmedia.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class StorageNodeRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String providerType; // LOCAL, MINIO, S3
    private Map<String, Object> connectionConfig;
    private Boolean isReadonly;
}
