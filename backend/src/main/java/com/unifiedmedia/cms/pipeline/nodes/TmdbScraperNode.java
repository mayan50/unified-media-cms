package com.unifiedmedia.cms.pipeline.nodes;

import com.unifiedmedia.cms.pipeline.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class TmdbScraperNode extends BaseProcessingNode {

    public TmdbScraperNode() {
        super("TmdbScraperNode", "TMDB 刮削", "🎥",
                "从 TMDB 获取视听作品元数据（标题、年份、封面）",
                List.of(),
                List.of());
    }

    @Override
    public boolean canExecute(TaskContext context) {
        return context.getAiTitle() != null;
    }

    @Override
    public void execute(TaskContext context) throws ArbitrationRequiredException {
        String title = context.getAiTitle();
        log.info("[TmdbScraperNode] Searching TMDB for: {}", title);
        // TODO: TMDB API integration
        List<Map<String, Object>> candidates = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("title", title);
        result.put("source", "tmdb");
        result.put("year", context.getFinalPublishYear());
        candidates.add(result);
        if (candidates.size() > 1) {
            throw new ArbitrationRequiredException(getNodeName(), "Multiple TMDB results found", candidates);
        }
        if (!candidates.isEmpty()) context.setSelectedResult(candidates.get(0));
        log.info("[TmdbScraperNode] Found: {}", candidates);
    }
}
