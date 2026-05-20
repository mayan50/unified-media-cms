package com.unifiedmedia.cms.pipeline.nodes.processing;

import com.unifiedmedia.cms.pipeline.core.annotation.NodeDef;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.PipelineKeys;
import com.unifiedmedia.cms.pipeline.payload.ScrapeCandidate;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 通用元数据应用节点 — 读取仲裁后的 ScrapeCandidate，将结果写入 AssetDraft。
 */
@Slf4j
@NodeDef(name = "MetadataApplyNode", label = "元数据应用", icon = "📋", type = NodeType.PROCESSING)
public class MetadataApplyNode extends BaseProcessingNode implements EntityDataOperator {

    public MetadataApplyNode() {
        super("MetadataApplyNode", "元数据应用", "📋",
                "将刮削结果应用到资产草稿",
                List.of(), List.of());
    }

    @Override
    public boolean canExecute(TaskContext context) {
        return context.getPipelineData(PipelineKeys.SELECTED_RESULT, ScrapeCandidate.class) != null;
    }

    @Override
    public void execute(TaskContext context) {
        ScrapeCandidate candidate = context.getPipelineData(PipelineKeys.SELECTED_RESULT, ScrapeCandidate.class);
        if (candidate == null) return;

        AssetDraft draft = context.getAssetDraft();

        if (candidate.title() != null) draft.setTitle(candidate.title());
        if (candidate.summary() != null) draft.setSummary(candidate.summary());
        if (candidate.coverUrl() != null) draft.setCoverUrl(candidate.coverUrl());

        if (candidate.authors() != null) {
            candidate.authors().forEach(a -> draft.addCreator("作者", a));
        }
        if (candidate.tags() != null) {
            candidate.tags().forEach(t -> draft.addTag(t));
        }

        context.addLog("ok", "元数据应用完成: 标题=" + (candidate.title() != null ? candidate.title() : "无"));
    }
}
