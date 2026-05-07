package com.unifiedmedia.cms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "external_identifiers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"asset_id", "source"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalIdentifier {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "asset_id")
    private UUID assetId;

    private String source;

    private String identifier;

    private String url;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
