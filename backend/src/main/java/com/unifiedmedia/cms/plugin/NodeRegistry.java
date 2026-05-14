package com.unifiedmedia.cms.plugin;

import com.unifiedmedia.cms.pipeline.core.PipelineNode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点运行时注册表 — 唯一数据源。
 * 内置节点通过 PipelineAutoConfiguration 注册，外部插件通过 Pf4jPluginManager 注册。
 */
@Slf4j
public class NodeRegistry {

    private final Map<String, PipelineNode> nodes = new ConcurrentHashMap<>();
    private final Map<String, PipelineNode> builtInNodes = new ConcurrentHashMap<>();
    private final Map<String, List<String>> pluginToNodeNames = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> pluginMeta = new ConcurrentHashMap<>();

    /** 注册单个节点 */
    public void register(String pluginId, PipelineNode node) {
        String nodeName = node.getNodeName();

        if (builtInNodes.containsKey(nodeName)) {
            log.error("[安全拦截] 插件 '{}' 试图劫持系统内置核心节点: {}", pluginId, nodeName);
            throw new IllegalArgumentException("严重错误：禁止覆写系统内置节点 -> " + nodeName);
        }

        if (nodes.containsKey(nodeName)) {
            log.warn("[冲突警告] 节点名称已被注册，旧节点将被覆盖: {} (当前触发插件: {})", nodeName, pluginId);
        }

        nodes.put(nodeName, node);
        pluginToNodeNames.computeIfAbsent(pluginId, k -> new ArrayList<>()).add(nodeName);
        log.info("[NodeRegistry] Registered node: {} from plugin: {}", nodeName, pluginId);
    }

    public Set<String> getBuiltInNodeNames() {
        return Collections.unmodifiableSet(builtInNodes.keySet());
    }

    /** 注册单个节点（含插件元数据） */
    public void register(String pluginId, PipelineNode node, String pluginName, String pluginDesc) {
        register(pluginId, node, pluginName, pluginDesc, "");
    }

    /** 注册单个节点（含插件元数据及版本） */
    public void register(String pluginId, PipelineNode node, String pluginName, String pluginDesc, String pluginVersion) {
        register(pluginId, node);
        Map<String, String> meta = pluginMeta.computeIfAbsent(pluginId, k -> new LinkedHashMap<>());
        meta.put("name", pluginName);
        meta.put("description", pluginDesc);
        if (pluginVersion != null && !pluginVersion.isBlank()) {
            meta.put("version", pluginVersion);
        }
    }

    /** 获取插件元数据（名称、描述等） */
    public Map<String, Map<String, String>> getPluginMetas() {
        return Collections.unmodifiableMap(pluginMeta);
    }

    /** 卸载插件的所有节点 */
    public void unregisterPluginNodes(String pluginId) {
        List<String> nodeNames = pluginToNodeNames.remove(pluginId);
        if (nodeNames != null) {
            nodeNames.forEach(nodes::remove);
            log.info("[NodeRegistry] Unregistered {} nodes from plugin: {}", nodeNames.size(), pluginId);
        }
    }

    /** 注册内置节点（启动时调用，不支持卸载） */
    public void registerBuiltIn(PipelineNode node) {
        builtInNodes.put(node.getNodeName(), node);
        nodes.put(node.getNodeName(), node);
    }

    /** 获取所有活跃节点 */
    public Collection<PipelineNode> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    /** 获取插件数量（不含内置） */
    public int getPluginCount() {
        return pluginToNodeNames.size();
    }

    /** 获取插件到节点的映射（供管理页面展示） */
    public Map<String, List<String>> getPluginNodeMap() {
        return Collections.unmodifiableMap(pluginToNodeNames);
    }

    /** 节点名 → 标签 */
    public Map<String, String> getNodeLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        nodes.forEach((name, node) -> labels.put(name, node.getNodeLabel()));
        return labels;
    }
}
