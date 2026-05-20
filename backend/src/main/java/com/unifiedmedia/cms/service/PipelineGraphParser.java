package com.unifiedmedia.cms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 管线 DAG 图解析器 — 解析 JSON 图定义，执行 Kahn 拓扑排序，检测死循环。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineGraphParser {

    private final ObjectMapper objectMapper;

    public record DagResult(List<Map<String, Object>> nodes, List<Map<String, Object>> edges, List<String> sorted) {}

    /**
     * 解析并验证图结构，返回拓扑排序后的节点列表。
     *
     * @throws IllegalStateException 如果图中检测到死循环
     */
    public DagResult parseAndValidate(String graphJson) {
        Map<String, Object> g = parseGraph(graphJson);
        if (g == null) throw new IllegalArgumentException("无效的图结构 JSON");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) g.get("nodes");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> edges = (List<Map<String, Object>>) g.get("edges");
        if (nodes == null || nodes.isEmpty()) throw new IllegalArgumentException("图中没有节点");

        List<String> sorted = topoSort(nodes, edges != null ? edges : List.of());
        return new DagResult(nodes, edges != null ? edges : List.of(), sorted);
    }

    private Map<String, Object> parseGraph(String s) {
        if (s == null || s.isBlank()) return null;
        try { return objectMapper.readValue(s, new TypeReference<>() {}); }
        catch (Exception e) { log.error("parse graph failed", e); return null; }
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
