package com.unifiedmedia.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LlmConfigRequest {
    @NotBlank
    private String provider; // ollama or openai
    private String baseUrl;
    private String apiKey;
    private String modelName;
}
