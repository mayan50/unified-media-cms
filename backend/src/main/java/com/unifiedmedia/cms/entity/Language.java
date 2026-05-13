package com.unifiedmedia.cms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "languages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Language {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;  // zh-CN, en, ja, ko, etc.

    @Column(nullable = false, length = 50)
    private String name;  // 汉语（简体） / English

    @Column(name = "native_name", length = 50)
    private String nativeName;  // Chinese / Español

    @Column(name = "short_name", length = 20)
    private String shortName;  // 中文 / English

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
