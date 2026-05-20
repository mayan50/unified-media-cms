package com.unifiedmedia.cms.pipeline.core;
import com.unifiedmedia.cms.pipeline.nodes.io.FileSnifferNode;

import com.unifiedmedia.cms.pipeline.payload.NodeExecutionResult;
import com.unifiedmedia.cms.pipeline.payload.PipelineKeys;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 纯领域引擎 — 遍历 DAG 节点并执行，返回每个节点的执行结果。
 * 无 Spring 注解，不感知数据库和事务。
 */
@Slf4j
public class PipelineEngine {

    public List<NodeExecutionResult> run(List<PipelineNode> executionPlan,
                                          List<Map<String, Object>> graphNodes,
                                          List<String> sortedNodeIds,
                                          Map<String, PipelineNode> nodeMap,
                                          TaskContext context) {

        List<NodeExecutionResult> results = new ArrayList<>();

        for (PipelineNode node : executionPlan) {
            if (FileSnifferNode.NODE_NAME.equals(NodeMetaReader.getNodeName(node))) continue;

            injectNodeConfig(graphNodes, sortedNodeIds, NodeMetaReader.getNodeName(node), context);

            if (!node.canExecute(context)) {
                results.add(new NodeExecutionResult(
                        NodeMetaReader.getNodeName(node), NodeMetaReader.getNodeLabel(node), "SKIPPED", 0L,
                        List.copyOf(context.drainLogs()), "Precondition not met"));
                continue;
            }

            long start = System.currentTimeMillis();
            try {
                node.execute(context);
                results.add(new NodeExecutionResult(
                        NodeMetaReader.getNodeName(node), NodeMetaReader.getNodeLabel(node), "SUCCESS",
                        System.currentTimeMillis() - start,
                        List.copyOf(context.drainLogs()), null));
            } catch (Exception e) {
                results.add(new NodeExecutionResult(
                        NodeMetaReader.getNodeName(node), NodeMetaReader.getNodeLabel(node), "FAILED",
                        System.currentTimeMillis() - start,
                        List.copyOf(context.drainLogs()), e.getMessage()));
                break;  // 停止后续节点，但返回已收集的结果
            }
        }
        return results;
    }

    private void injectNodeConfig(List<Map<String, Object>> graphNodes, List<String> sortedNodeIds,
                                   String nodeName, TaskContext ctx) {
        for (String nodeId : sortedNodeIds) {
            Map<String, Object> def = findNodeDef(graphNodes, nodeId);
            if (nodeName.equals(def.get("name"))) {
                Object cfg = def.get("config");
                if (cfg instanceof Map<?,?> m) {
                    m.forEach((k, v) -> {
                        if (k instanceof String keyStr) ctx.setPipelineData(keyStr, v);
                    });
                }
                Object cond = def.get("condition");
                if (cond instanceof String s && !s.isBlank()) ctx.setPipelineData(PipelineKeys.ROUTER_CONDITION, s);
                break;
            }
        }
    }

    private Map<String, Object> findNodeDef(List<Map<String, Object>> nodes, String id) {
        return nodes.stream().filter(n -> id.equals(n.get("id"))).findFirst().orElse(Map.of());
    }
}
