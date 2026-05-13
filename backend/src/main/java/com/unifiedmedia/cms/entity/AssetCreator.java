package com.unifiedmedia.cms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "asset_creators", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"asset_id", "creator_id", "role"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetCreator {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @Column(length = 50)
    @Builder.Default
    private String role = "作者";

    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = java.time.LocalDateTime.now();
    }
}
