package com.unifiedmedia.cms.pipeline.nodes.scraper;
import com.unifiedmedia.cms.pipeline.core.annotation.NodeDef;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@NodeDef(name = "DoubanScraperNode", label = "豆瓣刮削", icon = "🌐", type = NodeType.PROCESSING, description = "从豆瓣自动补全图书元数据和封面图")
public class DoubanScraperNode extends BaseProcessingNode implements EntityDataOperator {

    private static final String DOUBAN_SEARCH_URL = "https://search.douban.com/book/subject_search";
    private static final int MAX_CANDIDATES = 5;
    private static final int TIMEOUT_MS = 15000;

    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";

    public DoubanScraperNode() {
        super("DoubanScraperNode", "豆瓣刮削", "🌐",
                "从豆瓣自动补全图书元数据和封面图",
                List.of(
                        ConfigFieldDef.text("cookie", "Cookie", "可选：豆瓣登录后的 Cookie，绕过反爬限制")
                ),
                List.of());
    }

    @Override
    public boolean canExecute(TaskContext context) {
        AssetDraft draft = context.getAssetDraft();
        return draft != null && draft.getTitle() != null && !draft.getTitle().isBlank();
    }

    @Override
    public void execute(TaskContext context) throws Exception {
        String originalTitle = context.getAssetDraft().getTitle();
        String cleanTitle = cleanSearchQuery(originalTitle);

        log.info("[DoubanScraperNode] Scraping douban for: {} (cleaned: {})", originalTitle, cleanTitle);

        String cookie = context.getPipelineData("cookie", String.class);
        List<ScrapeCandidate> candidates = scrapeDouban(cleanTitle, cookie);
        context.setPipelineData(PipelineKeys.SCRAPER_CANDIDATES, candidates);

        if (candidates.isEmpty()) {
            context.addLog("warn", "豆瓣搜索: '" + cleanTitle + "' → 无结果或抓取失败");
            log.warn("[DoubanScraperNode] No results found or blocked by anti-spider for: {}", cleanTitle);
            return;
        }

        if (candidates.size() == 1) {
            ScrapeCandidate selected = candidates.get(0);
            context.setPipelineData(PipelineKeys.SELECTED_RESULT, selected);
            applyResult(context, selected);
            context.addLog("ok", "豆瓣搜索: '" + cleanTitle + "' → 1个结果, 标题=" + selected.title());
            return;
        }

        throw new ArbitrationRequiredException(getNodeName(),
                "Found " + candidates.size() + " candidates for: " + cleanTitle,
                Map.of("aiSuggestion", Map.of("title", originalTitle,
                        "author", context.getPipelineData(PipelineKeys.AUTHORS, String.class)),
                       "candidates", candidates));
    }

    private List<ScrapeCandidate> scrapeDouban(String title, String cookie) {
        List<ScrapeCandidate> results = new ArrayList<>(MAX_CANDIDATES);
        try {
            String url = DOUBAN_SEARCH_URL + "?search_text=" + URLEncoder.encode(title, StandardCharsets.UTF_8) + "&cat=1001";
            if (cookie == null || cookie.isBlank()) {
                cookie = "bid=" + generateRandomBid();
            }

            Document doc = Jsoup.connect(url)
                    .userAgent(DEFAULT_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Cookie", cookie)
                    .timeout(TIMEOUT_MS)
                    .get();

            Elements items = doc.select(".item-root");

            for (int i = 0; i < Math.min(items.size(), MAX_CANDIDATES); i++) {
                Element item = items.get(i);

                String bookTitle = Optional.ofNullable(item.selectFirst(".title-text"))
                        .map(Element::text).map(String::trim).orElse(null);
                String doubanUrl = Optional.ofNullable(item.selectFirst("a.title-text"))
                        .map(e -> e.attr("href")).orElse(null);
                String coverUrl = Optional.ofNullable(item.selectFirst("img.cover"))
                        .map(e -> e.attr("src")).orElse(null);

                if (bookTitle == null && doubanUrl == null) continue;

                Map<String, String> extraData = new HashMap<>();
                if (doubanUrl != null) extraData.put("doubanUrl", doubanUrl);

                results.add(new ScrapeCandidate("douban", null, bookTitle, null, coverUrl, null, null, extraData));
            }
        } catch (HttpStatusException e) {
            log.error("[DoubanScraperNode] HTTP Error {}: Possibly blocked. URL: {}", e.getStatusCode(), e.getUrl());
        } catch (SocketTimeoutException e) {
            log.error("[DoubanScraperNode] Connection timeout after {}ms", TIMEOUT_MS);
        } catch (Exception e) {
            log.error("[DoubanScraperNode] Scraping failed unexpectedly", e);
        }
        return results;
    }

    private void applyResult(TaskContext context, ScrapeCandidate result) {
        AssetDraft draft = context.getAssetDraft();

        if (result.title() != null && (draft.getTitle() == null || draft.getTitle().isBlank())) {
            draft.setTitle(result.title());
        }

        if (result.coverUrl() != null && (draft.getCoverUrl() == null || draft.getCoverUrl().isBlank())) {
            draft.setCoverUrl(result.coverUrl());
        }
    }

    private String generateRandomBid() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 11; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String cleanSearchQuery(String query) {
        if (query == null) return "";
        return query.replaceAll("[《》\\[\\]()（）]", " ").replaceAll("\\s+", " ").trim();
    }
}
