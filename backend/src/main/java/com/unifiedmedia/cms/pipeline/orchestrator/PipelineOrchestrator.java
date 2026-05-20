package com.unifiedmedia.cms.pipeline.orchestrator;
import com.unifiedmedia.cms.pipeline.nodes.io.FileSnifferNode;

import com.unifiedmedia.cms.entity.Asset;
import com.unifiedmedia.cms.entity.BookDetail;
import com.unifiedmedia.cms.entity.MediaDetail;
import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.NodeExecutionResult;
import com.unifiedmedia.cms.pipeline.payload.PipelineKeys;
import com.unifiedmedia.cms.repository.AssetRepository;
import com.unifiedmedia.cms.repository.BookDetailRepository;
import com.unifiedmedia.cms.pipeline.mapper.AssetDraftMapper;
import com.unifiedmedia.cms.pipeline.mapper.MediaDetailDraftMapper;
import com.unifiedmedia.cms.pipeline.sync.AssetSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

/**
 * 流水线编排器 — 采用 Work-Commit 两阶段模型。
 * <p>
 * Work 阶段（无事务）：调用节点 {@code execute()}，允许网络 IO、LLM 等耗时操作。
 * Commit 阶段（短事务）：检测 {@link EntityDataOperator} 标记，自动将 {@link AssetDraft}
 * 修改合并到数据库实体，并处理多对多关联同步。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineOrchestrator {

    private final TransactionTemplate txTemplate;
    private final AssetRepository assetRepository;
    private final BookDetailRepository bookDetailRepository;
    private final AssetDraftMapper assetDraftMapper;
    private final MediaDetailDraftMapper detailDraftMapper;
    private final AssetSyncService assetSyncService;

    /** 系统隐式节点：头尾夹心饼干 */
    private final PipelineNode fetchSandboxNode;
    private final PipelineNode sinkUploadNode;

    /**
     * 执行流水线节点计划。
     *
     * @param executionPlan  待执行节点列表
     * @param graphNodes     图节点定义（用于注入 config）
     * @param sortedNodeIds  拓扑排序
     * @param nodeMap        节点名 → 节点实例
     * @param context        流水线上下文（含 AssetDraft）
     * @return 每个节点的执行结果
     */
    public List<NodeExecutionResult> run(List<PipelineNode> executionPlan,
                                          List<Map<String, Object>> graphNodes,
                                          List<String> sortedNodeIds,
                                          Map<String, PipelineNode> nodeMap,
                                          PipelineTaskContext context) {

        List<NodeExecutionResult> results = new ArrayList<>();

        // ── 夹心饼干：组装实际执行列表 ──
        List<PipelineNode> actualPlan = new ArrayList<>();
        actualPlan.add(fetchSandboxNode);         // 系统隐式头节点
        for (PipelineNode node : executionPlan) {
            if (FileSnifferNode.NODE_NAME.equals(NodeMetaReader.getNodeName(node))) continue;
            actualPlan.add(node);
        }
        actualPlan.add(sinkUploadNode);           // 系统隐式尾节点

        for (PipelineNode node : actualPlan) {
            boolean isSystem = fetchSandboxNode == node || sinkUploadNode == node;
            String nodeName = NodeMetaReader.getNodeName(node);

            if (!isSystem) {
                injectNodeConfig(graphNodes, sortedNodeIds, nodeName, context);
            }

            if (!node.canExecute(context)) {
                results.add(new NodeExecutionResult(
                        nodeName, NodeMetaReader.getNodeLabel(node), "SKIPPED", 0L,
                        List.copyOf(context.drainLogs()), "Precondition not met"));
                if (!isSystem) continue;
            }

            long start = System.currentTimeMillis();
            try {
                node.execute(context);

                if (node instanceof EntityDataOperator && context.getAssetDraft() != null) {
                    txTemplate.executeWithoutResult(status -> {
                        Asset managedAsset = assetRepository.findById(context.getAssetId())
                                .orElseThrow(() -> new IllegalStateException("Asset not found: " + context.getAssetId()));
                        assetDraftMapper.mergeDraftToEntity(context.getAssetDraft(), managedAsset);

                        MediaDetail managedDetail = bookDetailRepository.findByAssetId(context.getAssetId()).orElse(null);
                        if (context.getDetailDraft() != null && managedDetail != null) {
                            detailDraftMapper.mergeDraftToEntity(context.getDetailDraft(), managedDetail);
                            if (managedDetail instanceof BookDetail bd) bookDetailRepository.save(bd);
                        }

                        assetSyncService.syncPendingData(context.getAssetDraft(), managedAsset);
                        assetRepository.save(managedAsset);

                        context.setAssetDraft(assetDraftMapper.createDraftFromEntity(managedAsset));
                        if (managedDetail != null) {
                            context.setDetailDraft(detailDraftMapper.createDraftFromEntity(managedDetail));
                        }
                    });
                }

                results.add(new NodeExecutionResult(
                        nodeName, NodeMetaReader.getNodeLabel(node), "SUCCESS",
                        System.currentTimeMillis() - start,
                        List.copyOf(context.drainLogs()), null));
            } catch (Exception e) {
                log.error("[PipelineOrchestrator] Node {} failed: {}", nodeName, e.getMessage());
                results.add(new NodeExecutionResult(
                        nodeName, NodeMetaReader.getNodeLabel(node), "FAILED",
                        System.currentTimeMillis() - start,
                        List.copyOf(context.drainLogs()), e.getMessage()));
                break;
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
                if (cfg instanceof Map<?, ?> m) {
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
