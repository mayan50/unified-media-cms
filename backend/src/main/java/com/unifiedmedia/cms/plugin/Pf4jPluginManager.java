package com.unifiedmedia.cms.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.unifiedmedia.cms.pipeline.core.PipelineNode;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.*;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

/**
 * PF4J 封装 — 只取 ClassLoader + 生命周期，不污染 PipelineNode 合约。
 */
@Slf4j
public class Pf4jPluginManager {

    private final PluginManager pluginManager;
    private final NodeRegistry nodeRegistry;
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    public Pf4jPluginManager(Path pluginsDir, NodeRegistry nodeRegistry) {
        this(pluginsDir, nodeRegistry, Pf4jPluginManager.class.getClassLoader());
    }

    public Pf4jPluginManager(Path pluginsDir, NodeRegistry nodeRegistry, ClassLoader parentClassLoader) {
        this.pluginManager =  new DefaultPluginManager(pluginsDir);;
        this.nodeRegistry = nodeRegistry;
    }

    public Pf4jPluginManager(PluginManager pluginManager, NodeRegistry nodeRegistry) {
        this.pluginManager = pluginManager;
        this.nodeRegistry = nodeRegistry;
    }

    /** 扫描并加载所有插件，返回新增的节点数 */
    public int loadAllPlugins() {
        pluginManager.loadPlugins();
        // We don't use PF4J Plugin lifecycle — only ClassLoader
        int count = 0;
        for (PluginWrapper wrapper : pluginManager.getPlugins()) {
            try {
                count += loadPluginNodes(wrapper);
            } catch (Exception e) {
                log.error("[Pf4jPluginManager] Failed to load plugin: {}", wrapper.getPluginId(), e);
            }
        }
        log.info("[Pf4jPluginManager] Loaded {} plugins, {} nodes", pluginManager.getPlugins().size(), count);
        return count;
    }

    /** 热重载：卸载全部插件 → 重新加载 */
    public int reloadAllPlugins() {
        for (PluginWrapper w : pluginManager.getPlugins()) {
            nodeRegistry.unregisterPluginNodes(w.getPluginId());
            pluginManager.unloadPlugin(w.getPluginId());
        }
        return loadAllPlugins();
    }

    /** 加载单个插件的节点 */
    @SuppressWarnings("unchecked")
    private int loadPluginNodes(PluginWrapper wrapper) throws Exception {
        ClassLoader pluginLoader = wrapper.getPluginClassLoader();
        String pluginId = wrapper.getPluginId();

        // 尝试读 plugin.yml
        InputStream yamlStream = pluginLoader.getResourceAsStream("plugin.yml");
        if (yamlStream == null) {
            yamlStream = pluginLoader.getResourceAsStream("plugin.yaml");
        }
        if (yamlStream == null) {
            // 回退到 plugin.properties 的标准 PF4J 方式
            String pluginClass = wrapper.getDescriptor().getPluginDescription();
            if (pluginClass == null || pluginClass.isBlank()) {
                log.debug("[Pf4jPluginManager] No plugin.yml or plugin.class for {}", pluginId);
                return 0;
            }
            Class<?> clazz = Class.forName(pluginClass, true, pluginLoader);
            PipelineNode node = (PipelineNode) clazz.getDeclaredConstructor().newInstance();
            nodeRegistry.register(pluginId, node, pluginId, "", wrapper.getDescriptor().getVersion());
            return 1;
        }

        Map<String, Object> config = YAML.readValue(yamlStream, Map.class);
        Object pluginSection = config.get("plugin");
        if (!(pluginSection instanceof Map<?, ?> pluginMap)) return 0;

        String pluginName = Objects.toString(pluginMap.get("name"), "");
        String pluginDesc = Objects.toString(pluginMap.get("description"), "");
        String pluginVersion = Objects.toString(pluginMap.get("version"), "");

        Object nodesObj = pluginMap.get("nodes");
        if (!(nodesObj instanceof List<?> nodeList)) return 0;

        int count = 0;
        for (Object item : nodeList) {
            if (!(item instanceof Map<?, ?> nodeDef)) continue;
            Object className = nodeDef.get("class");
            if (!(className instanceof String classNameStr)) continue;

            Class<?> clazz = Class.forName(classNameStr, true, pluginLoader);
            PipelineNode node = (PipelineNode) clazz.getDeclaredConstructor().newInstance();
            nodeRegistry.register(pluginId, node, pluginName, pluginDesc, pluginVersion);
            count++;
        }
        return count;
    }

    public void stop() {
        // PF4J lifecycle not used — only ClassLoader
    }
}
