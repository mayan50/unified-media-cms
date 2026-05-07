package com.unifiedmedia.cms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "external_ratings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"asset_id", "source"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalRating {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "asset_id")
    private UUID assetId;

    private String source;

    @Column(precision = 3, scale = 1)
    private BigDecimal score;

    private Integer count;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
