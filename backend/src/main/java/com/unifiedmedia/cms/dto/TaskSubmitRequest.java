package com.unifiedmedia.cms.dto;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class TaskSubmitRequest {
    private String name;
    private UUID templateId;

    /** 输入路径：{ storage_node_id, path }，模板含 INPUT 节点时需要 */
    private Map<String, Object> inputPath;

    /** 输出路径：{ storage_node_id, path }，模板含 OUTPUT 节点时需要 */
    private Map<String, Object> outputPath;
}
