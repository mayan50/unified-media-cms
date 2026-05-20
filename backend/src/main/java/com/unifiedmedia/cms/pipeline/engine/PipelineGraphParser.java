package com.unifiedmedia.cms.pipeline.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 管线 DAG 图解析器 — 接收已反序列化的 Map 图定义，执行 Kahn 拓扑排序，检测死循环。
 */
@Slf4j
@Component
public class PipelineGraphParser {

    public record DagResult(List<Map<String, Object>> nodes, List<Map<String, Object>> edges, List<String> sorted) {}

    /**
     * 解析并验证图结构，返回拓扑排序后的节点列表。
     *
     * @throws IllegalStateException 如果图中检测到死循环
     */
    @SuppressWarnings("unchecked")
    public DagResult parseAndValidate(Map<String, Object> graph) {
        if (graph == null || graph.isEmpty()) throw new IllegalArgumentException("无效的图结构");

        List<Map<String, Object>> nodes = (List<Map<String, Object>>) graph.get("nodes");
        List<Map<String, Object>> edges = (List<Map<String, Object>>) graph.get("edges");
        if (nodes == null || nodes.isEmpty()) throw new IllegalArgumentException("图中没有节点");

        List<String> sorted = topoSort(nodes, edges != null ? edges : List.of());
        return new DagResult(nodes, edges != null ? edges : List.of(), sorted);
    }

    /**
     * Kahn 算法拓扑排序。入度为 0 的节点进队，每次出队后拆桥（下游入度减 1）。
     * 如果排序结果数量 < 总节点数，说明存在死循环。
     */
    private List<String> topoSort(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        Map<String, Integer> inDeg = new LinkedHashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        for (Map<String, Object> n : nodes) {
            String id = (String) n.get("id");
            inDeg.put(id, 0);
            adj.put(id, new ArrayList<>());
        }
        for (Map<String, Object> e : edges) {
            String s = (String) e.get("source"), t = (String) e.get("target");
            if (adj.containsKey(s) && inDeg.containsKey(t)) {
                adj.get(s).add(t);
                inDeg.merge(t, 1, Integer::sum);
            }
        }
        Queue<String> q = new LinkedList<>();
        for (var e : inDeg.entrySet()) if (e.getValue() == 0) q.add(e.getKey());
        List<String> r = new ArrayList<>();
        while (!q.isEmpty()) {
            String n = q.poll();
            r.add(n);
            for (String next : adj.getOrDefault(n, List.of())) {
                int d = inDeg.get(next) - 1;
                inDeg.put(next, d);
                if (d == 0) q.add(next);
            }
        }
        if (r.size() != nodes.size()) throw new IllegalStateException("Cycle detected");
        return r;
    }
}
