package com.unifiedmedia.cms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "asset_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetFile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "storage_node_id", nullable = false)
    private UUID storageNodeId;

    @Column(name = "file_format", nullable = false, length = 50)
    private String fileFormat;

    @Column(name = "relative_path", nullable = false)
    private String relativePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
