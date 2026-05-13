package com.unifiedmedia.cms.dto;

import lombok.Data;
import java.util.Map;
import java.util.UUID;

@Data
public class JobSubmitRequest {
    private String name;
    private UUID templateId;
    private Map<String, Object> inputPath;
    private Map<String, Object> outputPath;
}
