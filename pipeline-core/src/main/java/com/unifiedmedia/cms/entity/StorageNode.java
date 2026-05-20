package com.unifiedmedia.cms.entity;

import lombok.Data;

import java.util.UUID;

/**
 * StorageNode 实体 stub — pipeline-core 中仅含 SPI 所需的字段。
 * 完整的 JPA 实体在 backend 模块中。
 */
@Data
public class StorageNode {

    private UUID id;
    private String name;
    private String providerType;           // LOCAL, MINIO, S3, WEBDAV, OSS
    private String connectionConfig;       // JSON string
    private Boolean isReadonly;
}
