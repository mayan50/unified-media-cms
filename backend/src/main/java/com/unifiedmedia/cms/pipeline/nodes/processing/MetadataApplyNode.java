package com.unifiedmedia.cms.pipeline.nodes.processing;

import com.unifiedmedia.cms.entity.Asset;
import com.unifiedmedia.cms.entity.MediaDetail;
import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.PipelineKeys;
import com.unifiedmedia.cms.pipeline.payload.ScrapeCandidate;
import com.unifiedmedia.cms.pipeline.spi.CandidateApplier;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 通用元数据应用节点 — 读取仲裁后的 ScrapeCandidate，
 * 标量字段直写实体，集合意图写入 PipelineData，detail 字段委托 CandidateApplier。
 */
@Slf4j
public class MetadataApplyNode extends BaseProcessingNode {

    private final List<CandidateApplier> appliers;

    public MetadataApplyNode() {
        this(List.of());
    }

    public MetadataApplyNode(List<CandidateApplier> appliers) {
        super("MetadataApplyNode", "元数据应用", "📋",
                "将刮削结果应用到资产实体",
                List.of(), List.of());
        this.appliers = appliers;
    }

    @Override
    public boolean canExecute(TaskContext context) {
        return context.getPipelineData(PipelineKeys.SELECTED_RESULT, ScrapeCandidate.class) != null;
    }

    @Override
    public void execute(TaskContext context) {
        ScrapeCandidate candidate = context.getPipelineData(PipelineKeys.SELECTED_RESULT, ScrapeCandidate.class);
        if (candidate == null) return;

        Asset asset = context.getAsset();

        if (candidate.title() != null) asset.updateTitle(candidate.title());
        if (candidate.summary() != null) asset.updateSummary(candidate.summary());
        if (candidate.coverUrl() != null) asset.updateCoverUrl(candidate.coverUrl());

        setIntentIfUnlocked(context, "authors", PipelineKeys.AUTHORS, candidate.authors());
        setIntentIfUnlocked(context, "tags", PipelineKeys.TAGS, candidate.tags());

        MediaDetail detail = context.getDetail(MediaDetail.class);
        if (detail != null) {
            for (CandidateApplier applier : appliers) {
                if (applier.supports(detail.getClass())) {
                    applier.apply(detail, candidate);
                    break;
                }
            }
        }

        context.addLog("ok", "元数据应用完成: 标题=" + (candidate.title() != null ? candidate.title() : "无"));
    }
}
