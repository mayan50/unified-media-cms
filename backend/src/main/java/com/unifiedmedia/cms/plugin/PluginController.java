package com.unifiedmedia.cms.plugin;

import com.unifiedmedia.cms.pipeline.core.PipelineNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/plugins")
public class PluginController {

    private final NodeRegistry nodeRegistry;
    private final Pf4jPluginManager pf4jPluginManager;

    public PluginController(NodeRegistry nodeRegistry, Pf4jPluginManager pf4jPluginManager) {
        this.nodeRegistry = nodeRegistry;
        this.pf4jPluginManager = pf4jPluginManager;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listPlugins() {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, List<String>> pluginMap = nodeRegistry.getPluginNodeMap();
        Map<String, Map<String, String>> metaMap = nodeRegistry.getPluginMetas();
        for (var entry : pluginMap.entrySet()) {
            String pluginId = entry.getKey();
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("pluginId", pluginId);
            p.put("nodeCount", entry.getValue().size());
            p.put("nodeNames", entry.getValue());
            p.put("running", true);
            // 附加中文名、描述、版本
            Map<String, String> meta = metaMap.get(pluginId);
            if (meta != null) {
                p.put("name", meta.getOrDefault("name", ""));
                p.put("description", meta.getOrDefault("description", ""));
                String version = meta.get("version");
                if (version != null && !version.isBlank()) {
                    p.put("version", version);
                }
            }
            result.add(p);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reload() {
        int count = pf4jPluginManager.reloadAllPlugins();
        return ResponseEntity.ok(Map.of("status", "reloaded", "nodesAdded", count));
    }
}
