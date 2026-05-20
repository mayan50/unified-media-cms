package com.unifiedmedia.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class TemplateRequest {
    @NotBlank
    private String name;
    private String description;
    private Map<String, Object> graphPayload;
    private Boolean isDefault;
}
