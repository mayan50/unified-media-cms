package com.unifiedmedia.cms.service;

import com.unifiedmedia.cms.pipeline.ConfigFieldDef;
import com.unifiedmedia.cms.pipeline.NodeType;
import com.unifiedmedia.cms.pipeline.PipelineNode;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 节点注册表服务 — 动态发现所有 PipelineNode bean，
 * 按节点类型分组返回，配置字段由各节点自描述。
 */
@Service
@RequiredArgsConstructor
public class NodeRegistryService {

    private final List<PipelineNode> allNodes;

    /**
     * 返回按 NodeType 分组的完整注册表
     */
    public List<CategoryDef> getRegistry() {
        // 按 NodeType 分组
        Map<NodeType, List<NodeDef>> grouped = allNodes.stream()
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
        return allNodes.stream()
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
