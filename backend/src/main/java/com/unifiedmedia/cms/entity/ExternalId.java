package com.unifiedmedia.cms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "external_ids", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"asset_id", "source"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalId {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "asset_id")
    private UUID assetId;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(nullable = false, length = 100)
    private String identifier;

    private String url;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
