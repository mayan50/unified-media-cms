package com.unifiedmedia.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class TemplateRequest {
    @NotBlank
    private String name;
    private String description;
    private Object graphPayload;           // DAG 图: {"nodes": [...], "edges": [...]}
    private Boolean isDefault;
}
