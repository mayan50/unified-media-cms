package com.unifiedmedia.cms.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TaskResumeRequest {
    private Map<String, Object> mergedPayload;
}
