package com.unifiedmedia.cms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "external_scores", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"asset_id", "source"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalScore {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "asset_id")
    private UUID assetId;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(precision = 3, scale = 1)
    private BigDecimal score;

    private Integer count;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
