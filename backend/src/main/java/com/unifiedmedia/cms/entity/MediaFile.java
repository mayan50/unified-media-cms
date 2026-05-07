package com.unifiedmedia.cms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "media_files")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "storage_node_id", nullable = false)
    private UUID storageNodeId;

    @Column(name = "file_format", nullable = false, length = 50)
    private String fileFormat; // TXT, EPUB

    @Column(name = "relative_path", nullable = false)
    private String relativePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = true;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
    }
}
