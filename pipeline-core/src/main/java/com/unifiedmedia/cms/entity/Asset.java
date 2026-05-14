package com.unifiedmedia.cms.entity;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Asset entity stub for pipeline-core — contains only the fields and methods
 * referenced by the pipeline API and node implementations.
 * The full JPA entity lives in the backend module.
 */
@Data
public class Asset {

    private UUID id;
    private String title;
    private String summary;
    private String coverUrl;
    private int publishYear;
    private String mediaType;
    private Set<String> lockedFields = new HashSet<>();

    public void updateTitle(String v) {
        if (v != null && !lockedFields.contains("title")) this.title = v;
    }

    public void updateSummary(String v) {
        if (v != null && !lockedFields.contains("summary")) this.summary = v;
    }

    public void updateCoverUrl(String v) {
        if (v != null && !lockedFields.contains("coverUrl")) this.coverUrl = v;
    }

    public void updatePublishYear(Integer v) {
        if (v != null && !lockedFields.contains("publishYear")) this.publishYear = v;
    }

    public boolean isFieldLocked(String fieldName) {
        return lockedFields.contains(fieldName);
    }
}
