package com.unifiedmedia.cms.poc;

import com.unifiedmedia.cms.pipeline.core.PipelineNode;
import com.unifiedmedia.cms.pipeline.core.StandardTaskContext;
import com.unifiedmedia.cms.pipeline.core.TaskContext;
import org.pf4j.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/poc")
public class PocController {

    private static final String PLUGIN_DIR = "/tmp/um-plugins";

    @GetMapping("/reload-and-run")
    public ResponseEntity<Map<String, Object>> reloadAndRun() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> outputs = new ArrayList<>();

        PluginManager pluginManager = new DefaultPluginManager(Path.of(PLUGIN_DIR));

        try {
            // 1. 加载插件
            pluginManager.loadPlugins();
            pluginManager.startPlugins();
            List<PluginWrapper> plugins = pluginManager.getPlugins();
            result.put("pluginsFound", plugins.size());

            if (plugins.isEmpty()) {
                result.put("status", "no_plugins");
                return ResponseEntity.ok(result);
            }

            // 2. 遍历每个插件，反射实例化节点
            for (PluginWrapper wrapper : plugins) {
                ClassLoader cl = wrapper.getPluginClassLoader();
                String pluginId = wrapper.getPluginId();
                result.put("pluginId", pluginId);

                // 从 plugin.properties 读取节点类名
                String nodeClassName = wrapper.getDescriptor().getPluginDescription();
                if (nodeClassName == null || nodeClassName.isBlank()) {
                    nodeClassName = "com.unifiedmedia.cms.poc.DummyNode";
                }

                try {
                    Class<?> nodeClass = Class.forName(nodeClassName, true, cl);
                    PipelineNode node = (PipelineNode) nodeClass.getDeclaredConstructor().newInstance();

                    // 3. 执行节点
                    TaskContext ctx = new StandardTaskContext(UUID.randomUUID(), null);
                    node.execute(ctx);

                    List<String> logs = ctx.drainLogs();
                    outputs.addAll(logs);
                    result.put("status", "executed");
                    result.put("nodeLabel", node.getNodeLabel());

                } catch (Exception e) {
                    result.put("status", "error");
                    result.put("error", e.getMessage());
                }

                // 4. 卸载插件
                pluginManager.unloadPlugin(wrapper.getPluginId());
            }

        } finally {
            pluginManager.stopPlugins();
        }

        result.put("outputs", outputs);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listPlugins() {
        PluginManager pm = new DefaultPluginManager(Path.of(PLUGIN_DIR));
        pm.loadPlugins();
        List<String> names = pm.getPlugins().stream()
                .map(w -> w.getPluginId() + " — " + w.getDescriptor().getVersion())
                .toList();
        pm.stopPlugins();
        return ResponseEntity.ok(Map.of("plugins", names, "dir", PLUGIN_DIR));
    }
}
