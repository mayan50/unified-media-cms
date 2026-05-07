package com.unifiedmedia.cms.pipeline.nodes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifiedmedia.cms.pipeline.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class DoubanScraperNode extends BaseProcessingNode {

    private final ObjectMapper objectMapper;
    private static final String DOUBAN_SEARCH_URL = "https://search.douban.com/book/subject_search";
    private static final int MAX_CANDIDATES = 5;

    public DoubanScraperNode(ObjectMapper objectMapper) {
        super("DoubanScraperNode", "豆瓣刮削", "🌐",
                "从豆瓣自动补全图书元数据和封面图",
                List.of(),
                List.of());
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canExecute(TaskContext context) {
        String title = context.getAiTitle();
        return title != null && !title.isBlank();
    }

    @Override
    public void execute(TaskContext context) throws Exception {
        String title = context.getAiTitle();
        log.info("[DoubanScraperNode] Scraping douban for: {}", title);
        List<Map<String, Object>> candidates = scrapeDouban(title);
        context.setScraperCandidates(candidates);
        if (candidates.isEmpty()) { log.warn("[DoubanScraperNode] No results"); return; }
        if (candidates.size() == 1) {
            context.setSelectedResult(candidates.get(0));
            applyResult(context, candidates.get(0));
            return;
        }
        throw new ArbitrationRequiredException(getNodeName(),
                "Found " + candidates.size() + " candidates for: " + title,
                Map.of("aiSuggestion", Map.of("title", context.getAiTitle(), "author", context.getAiAuthor()),
                       "candidates", candidates));
    }

    private List<Map<String, Object>> scrapeDouban(String title) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            String url = DOUBAN_SEARCH_URL + "?search_text=" + URLEncoder.encode(title, StandardCharsets.UTF_8) + "&cat=1001";
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .timeout(10000).get();
            Elements items = doc.select(".item-root");
            for (int i = 0; i < Math.min(items.size(), MAX_CANDIDATES); i++) {
                Element item = items.get(i);
                Map<String, Object> candidate = new HashMap<>();
                Element titleEl = item.selectFirst(".title-text");
                if (titleEl != null) candidate.put("title", titleEl.text().trim());
                Element linkEl = item.selectFirst("a.title-text");
                if (linkEl != null) candidate.put("doubanUrl", linkEl.attr("href"));
                Element metaEl = item.selectFirst(".meta.abstract");
                if (metaEl != null) candidate.put("meta", metaEl.text().trim());
                Element coverEl = item.selectFirst("img.cover");
                if (coverEl != null) candidate.put("coverUrl", coverEl.attr("src"));
                if (!candidate.isEmpty()) results.add(candidate);
            }
        } catch (Exception e) { log.error("[DoubanScraperNode] Scraping failed: {}", e.getMessage()); }
        return results;
    }

    private void applyResult(TaskContext context, Map<String, Object> result) {
        if (result.containsKey("title") && context.getFinalTitle() == null)
            context.setFinalTitle((String) result.get("title"));
        if (result.containsKey("coverUrl") && context.getFinalCoverUrl() == null)
            context.setFinalCoverUrl((String) result.get("coverUrl"));
        if (result.containsKey("meta")) {
            var m = java.util.regex.Pattern.compile("(\\d{4})").matcher((String) result.get("meta"));
            if (m.find() && context.getFinalPublishYear() == null)
                context.setFinalPublishYear(Integer.parseInt(m.group(1)));
        }
    }
}
