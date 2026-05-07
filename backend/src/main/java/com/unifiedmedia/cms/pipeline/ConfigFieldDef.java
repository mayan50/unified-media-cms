package com.unifiedmedia.cms.pipeline;

import lombok.Builder;

/**
 * 节点配置字段定义 — 由节点自描述，前端据此动态渲染表单
 */
@Builder
public record ConfigFieldDef(
        String key,
        String label,
        String type,           // text | number | textarea | storage_path
        String defaultValue,
        String selectableType  // file | directory | both (仅 storage_path 类型使用)
) {
    public static ConfigFieldDef text(String key, String label, String defaultValue) {
        return new ConfigFieldDef(key, label, "text", defaultValue, null);
    }

    public static ConfigFieldDef number(String key, String label, String defaultValue) {
        return new ConfigFieldDef(key, label, "number", defaultValue, null);
    }

    public static ConfigFieldDef textarea(String key, String label, String defaultValue) {
        return new ConfigFieldDef(key, label, "textarea", defaultValue, null);
    }

    public static ConfigFieldDef storagePath(String key, String label, String defaultValue, String selectableType) {
        return new ConfigFieldDef(key, label, "storage_path", defaultValue, selectableType);
    }
}
