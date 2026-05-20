package com.unifiedmedia.cms.pipeline.nodes.scraper;
import com.unifiedmedia.cms.pipeline.core.annotation.NodeDef;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@NodeDef(name = "TmdbScraperNode", label = "TMDB 刮削", icon = "🎥", type = NodeType.PROCESSING)
public class TmdbScraperNode extends BaseProcessingNode {

    public TmdbScraperNode() {
        super("TmdbScraperNode", "TMDB 刮削", "🎥",
                "从 TMDB 获取视听作品元数据（标题、年份、封面）",
                List.of(),
                List.of());
    }

    @Override
    public boolean canExecute(TaskContext context) {
        AssetDraft draft = context.getAssetDraft();
        return draft != null && draft.getTitle() != null && !draft.getTitle().isBlank();
    }

    @Override
    public void execute(TaskContext context) throws ArbitrationRequiredException {
        String title = context.getAssetDraft().getTitle();
        log.info("[TmdbScraperNode] Searching TMDB for: {}", title);
        // TODO: TMDB API integration
        List<ScrapeCandidate> candidates = new ArrayList<>();
        ScrapeCandidate result = new ScrapeCandidate(
                "tmdb", null, title, null, null, null, null, Map.of());
        candidates.add(result);
        if (candidates.size() > 1) {
            throw new ArbitrationRequiredException(getNodeName(), "Multiple TMDB results found", candidates);
        }
        if (!candidates.isEmpty()) context.setPipelineData(PipelineKeys.SELECTED_RESULT, candidates.get(0));
        context.addLog(candidates.isEmpty() ? "warn" : "ok", "TMDB搜索: '" + title + "' → " + candidates.size() + "个结果");
        log.info("[TmdbScraperNode] Found: {}", candidates);
    }
}
