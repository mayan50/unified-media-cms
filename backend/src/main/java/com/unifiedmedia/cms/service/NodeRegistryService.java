package com.unifiedmedia.cms.service;

import com.unifiedmedia.cms.pipeline.core.ConfigFieldDef;
import com.unifiedmedia.cms.pipeline.core.NodeType;
import com.unifiedmedia.cms.plugin.NodeRegistry;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 节点注册表服务 — 从动态 NodeRegistry 实时获取所有节点，
 * 按节点类型分组返回，配置字段由各节点自描述。
 */
@Service
@RequiredArgsConstructor
public class NodeRegistryService {

    private final NodeRegistry registry;

    /** 简单列表：{name, className}，供 NodeController.getAvailableNodes() */
    public List<Map<String, Object>> getAvailableNodes() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (var node : registry.getNodes()) {
            Map<String, Object> info = new HashMap<>();
            info.put("name", node.getNodeName());
            info.put("className", node.getClass().getSimpleName());
            result.add(info);
        }
        return result;
    }

    /**
     * 返回按 NodeType 分组的完整注册表
     */
    public List<CategoryDef> getRegistry() {
        // 按 NodeType 分组 — 每次请求实时从动态注册表读取
        Map<NodeType, List<NodeDef>> grouped = registry.getNodes().stream()
                .map(node -> NodeDef.builder()
                        .name(node.getNodeName())
                        .label(node.getNodeLabel())
                        .icon(node.getNodeIcon())
                        .description(node.getDescription())
                        .nodeType(node.getNodeType())
                        .hasConfig(node.getConfigSchema() != null && !node.getConfigSchema().isEmpty())
                        .configSchema(node.getConfigSchema() != null ? mapConfigFields(node.getConfigSchema()) : List.of())
                        .uiSummaryKeys(node.getUiSummaryKeys() != null ? node.getUiSummaryKeys() : List.of())
                        .available(true)
                        .build())
                .collect(Collectors.groupingBy(
                        NodeDef::getNodeType,
                        LinkedHashMap::new,
                        Collectors.toList()));

        // 转成前端需要的 CategoryDef 格式，按类型排序
        return Arrays.stream(NodeType.values())
                .filter(grouped::containsKey)
                .map(type -> new CategoryDef(
                        type.name().toLowerCase(),
                        type.getLabel(),
                        type.getIcon(),
                        grouped.get(type)))
                .collect(Collectors.toList());
    }

    /**
     * 按名称查找单个节点定义
     */
    public Optional<NodeDef> findNode(String name) {
        return registry.getNodes().stream()
                .filter(n -> n.getNodeName().equals(name))
                .findFirst()
                .map(n -> NodeDef.builder()
                        .name(n.getNodeName())
                        .label(n.getNodeLabel())
                        .icon(n.getNodeIcon())
                        .description(n.getDescription())
                        .nodeType(n.getNodeType())
                        .hasConfig(n.getConfigSchema() != null && !n.getConfigSchema().isEmpty())
                        .configSchema(n.getConfigSchema() != null ? mapConfigFields(n.getConfigSchema()) : List.of())
                        .uiSummaryKeys(n.getUiSummaryKeys() != null ? n.getUiSummaryKeys() : List.of())
                        .available(true)
                        .build());
    }

    private List<ConfigFieldDefDto> mapConfigFields(List<ConfigFieldDef> fields) {
        return fields.stream()
                .map(f -> new ConfigFieldDefDto(
                        f.key(), f.label(), f.type(), f.defaultValue(), f.selectableType()))
                .toList();
    }

    // ---- DTO ----

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class CategoryDef {
        private String key;
        private String label;
        private String icon;
        @Builder.Default
        private List<NodeDef> nodes = List.of();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class NodeDef {
        private String name;
        private String label;
        private String icon;
        private String description;
        private NodeType nodeType;
        private boolean hasConfig;
        @Builder.Default
        private List<ConfigFieldDefDto> configSchema = List.of();
        @Builder.Default
        private List<String> uiSummaryKeys = List.of();
        @Builder.Default
        private boolean available = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class ConfigFieldDefDto {
        private String key;
        private String label;
        private String type;
        private String defaultValue;
        private String selectableType;
    }
}
