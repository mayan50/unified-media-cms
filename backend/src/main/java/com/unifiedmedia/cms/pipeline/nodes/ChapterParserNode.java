package com.unifiedmedia.cms.pipeline.nodes;

import com.unifiedmedia.cms.pipeline.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 章节目录解析节点 — 从 TXT 文件中提取：
 * 1. 书名（文件头常见的《xxx》格式）
 * 2. 作者（文件头常见的 作者：xxx / Author: xxx）
 * 3. 简介/文案（文件头开篇内容摘要）
 * 4. 完整章节目录（支持 20+ 种章节格式）
 */
@Slf4j
@Component
public class ChapterParserNode extends BaseProcessingNode {

    public ChapterParserNode() {
        super("ChapterParserNode", "章节解析", "📑",
                "从 TXT 文本中智能提取书名、作者、简介和完整章节目录结构",
                List.of(
                        ConfigFieldDef.textarea("customPattern", "自定义正则", "可选：自定义章节匹配正则表达式，留空则使用内置规则")
                ),
                List.of());
    }

    // ---- 书名 / 作者 ----
    private static final Pattern TITLE_PATTERN = Pattern.compile("《([^》]+)》");
    private static final Pattern AUTHOR_PATTERN = Pattern.compile("(?:作者|Author|著)[：:\\s]+(.{1,40})", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUTHOR_ALT_PATTERN = Pattern.compile("^(.{2,8})\\s*著\\s*$", Pattern.MULTILINE);

    // ---- 章节目录（拆成独立 pattern 列表，避免巨型正则转义问题） ----
    private static final Pattern[] CHAPTER_PATTERNS = {
            // 第N章/回/部/卷/集/节
            Pattern.compile("^第\\s*\\d+\\s*[章回部卷集节][\\s：:].*"),
            Pattern.compile("^第\\s*[零一二三四五六七八九十百千万]+\\s*[章回部卷集节][\\s：:].*"),
            // 英文
            Pattern.compile("^Chapter\\s*\\d+.*", Pattern.CASE_INSENSITIVE),
            // 特殊章节名
            Pattern.compile("^(序章|尾声|后记|前言|楔子|终章|番外|文案)$"),
            // 中文数字序号 一、xxx
            Pattern.compile("^[一二三四五六七八九十百千万]{1,4}[\\s、]\\S"),
            // 数字 + 分隔符 1. xxx / 1、xxx / 1｜xxx
            Pattern.compile("^\\d+[.、｜|]\\s+\\S"),
            // 纯数字 1 xxx
            Pattern.compile("^\\d+\\s+\\S"),
            // 括号编号 (1) xxx
            Pattern.compile("^[（(]\\d+[)）]\\s*\\S"),
            // ◎xxx◎
            Pattern.compile("◎[^◎]+◎"),
            // ===xxx===
            Pattern.compile("===[^=]+==="),
            // ☆xxx
            Pattern.compile("^[☆★✦✧]\\s*\\S"),
            // 1 ◆ xxx
            Pattern.compile("^\\d+\\s+[☪☆★✦✧◆◇▲△▼▽■□●○]"),
            // 卷N / 第N卷
            Pattern.compile("^卷[一二三四五六七八九十百千萬\\d]+"),
            Pattern.compile("^第[一二三四五六七八九十百千萬\\d]+卷"),
            // 番N
            Pattern.compile("^番[一二三四五六七八九十百千萬\\d]+"),
            // 中文括号数字 （一）/ [二] / 〔三〕
            Pattern.compile("^[（(\\[［〔][一二三四五六七八九十百千万]+[)）\\]］〕]"),
    };

    @Override
    public boolean canExecute(TaskContext context) {
        String format = context.getDetectedFormat();
        return "TXT".equals(format) || "text/plain".equals(context.getDetectedMimeType());
    }

    @Override
    public void execute(TaskContext context) throws IOException {
        String path = context.getAbsolutePath();
        if (path == null) path = context.getRelativePath();

        String content = Files.readString(Path.of(path), StandardCharsets.UTF_8);
        String[] lines = content.split("\\R");

        log.info("[ChapterParserNode] Parsing file: {} lines", lines.length);

        // 1. 提取书名
        String title = extractTitle(content, path);
        if (title != null && context.getAiTitle() == null) {
            context.setAiTitle(title);
            log.info("[ChapterParserNode] Found title: {}", title);
        }

        // 2. 提取作者
        String author = extractAuthor(content);
        if (author != null && context.getAiAuthor() == null) {
            context.setAiAuthor(author);
            log.info("[ChapterParserNode] Found author: {}", author);
        }

        // 3. 提取简介（直到第一个章节标题之前的所有有效文本）
        Pattern customPat = buildCustomPattern(context);
        String intro = extractIntro(lines, customPat);
        if (intro != null && !intro.isBlank() && context.getAiSummary() == null) {
            context.setAiSummary(intro);
            log.info("[ChapterParserNode] Extracted intro: {} chars", intro.length());
        }

        // 4. 提取章节目录
        List<ChapterInfo> chapters = extractChapters(lines, customPat);
        List<Map<String, Object>> chapterList = chapters.stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("title", c.title);
                    m.put("lineNumber", c.lineNumber);
                    m.put("charOffset", c.charOffset);
                    m.put("format", c.format);
                    return m;
                })
                .collect(Collectors.toList());

        context.put("chapters", chapterList);
        context.put("chapterCount", chapters.size());
        context.put("chapterTitles", chapters.stream().map(c -> c.title).collect(Collectors.toList()));

        log.info("[ChapterParserNode] Found {} chapters", chapters.size());
    }

    // ==================== 书名提取 ====================

    private String extractTitle(String content, String filePath) {
        // 《xxx》格式
        Matcher m = TITLE_PATTERN.matcher(content);
        if (m.find()) {
            String t = m.group(1).trim();
            if (t.length() >= 1 && t.length() <= 80) return t;
        }
        // 文件名作为 fallback（去掉扩展名和路径）
        String fileName = Path.of(filePath).getFileName().toString();
        return fileName.replaceAll("\\.[^.]+$", "").replaceAll("[\\-_]", " ").trim();
    }

    // ==================== 作者提取 ====================

    private String extractAuthor(String content) {
        // 前 2000 字中查找
        String head = content.length() > 2000 ? content.substring(0, 2000) : content;

        Matcher m1 = AUTHOR_PATTERN.matcher(head);
        if (m1.find()) return cleanAuthor(m1.group(1));

        Matcher m2 = AUTHOR_ALT_PATTERN.matcher(head);
        if (m2.find()) return cleanAuthor(m2.group(1));

        return null;
    }

    private String cleanAuthor(String raw) {
        return raw.replaceAll("[：:\\s]", "").trim();
    }

    // ==================== 简介提取 ====================

    private String extractIntro(String[] lines, Pattern customPattern) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (matchesAnyChapter(trimmed, customPattern)) break;
            sb.append(trimmed).append('\n');
        }
        String intro = sb.toString().trim();
        return intro.length() > 10 ? intro : null;
    }

    // ==================== 章节目录提取 ====================

    private List<ChapterInfo> extractChapters(String[] lines, Pattern customPattern) {
        List<ChapterInfo> chapters = new ArrayList<>();
        int charOffset = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) { charOffset += lines[i].length() + 1; continue; }
            if (line.length() > 120) { charOffset += lines[i].length() + 1; continue; } // 过长非标题

            String format = matchChapter(line, customPattern);
            if (format != null) {
                // 合并连续的章节标题行（如 "第一章" 紧接着 "xxx"）
                String fullTitle = line;
                int skip = 0;
                if (i + 1 < lines.length) {
                    String next = lines[i + 1].trim();
                    if (!next.isEmpty() && !matchesAnyChapter(next, customPattern) && next.length() < 60) {
                        fullTitle = line + " " + next;
                        skip = 1;
                    }
                }

                chapters.add(new ChapterInfo(
                        fullTitle.replaceAll("\\s+", " ").trim(),
                        i + 1,
                        charOffset,
                        format
                ));
            }
            charOffset += lines[i].length() + 1;
        }

        // 智能过滤：如果匹配密度过大（中位间隔 < 5 行），自动提高门限
        if (chapters.size() > 3) {
            chapters = filterSparseChapters(chapters, lines.length);
        }

        return chapters;
    }

    private Pattern buildCustomPattern(TaskContext context) {
        Object val = context.get("customPattern");
        String patternStr = (val instanceof String s && !s.isBlank()) ? s.trim() : null;
        if (patternStr != null) {
            try { return Pattern.compile(patternStr, Pattern.MULTILINE); }
            catch (Exception e) { log.warn("[ChapterParserNode] Invalid custom regex: {}", e.getMessage()); }
        }
        return null;
    }

    private boolean matchesAnyChapter(String line, Pattern customPattern) {
        if (customPattern != null && customPattern.matcher(line).matches()) return true;
        for (Pattern p : CHAPTER_PATTERNS) {
            if (p.matcher(line).matches()) return true;
        }
        return false;
    }

    private String matchChapter(String line, Pattern customPattern) {
        if (customPattern != null && customPattern.matcher(line).matches()) return "custom";
        for (Pattern p : CHAPTER_PATTERNS) {
            if (p.matcher(line).matches()) {
                if (line.contains("章")) return "chapter";
                if (line.contains("回")) return "hui";
                if (line.contains("部")) return "bu";
                if (line.contains("卷")) return "volume";
                if (line.contains("集")) return "ji";
                if (line.contains("节")) return "jie";
                if (line.toLowerCase().contains("chapter")) return "english";
                return "special";
            }
        }
        return null;
    }

    /**
     * 过滤过于密集的匹配（正文中的数字行被误识别）
     */
    private List<ChapterInfo> filterSparseChapters(List<ChapterInfo> raw, int totalLines) {
        if (raw.size() <= 1) return raw;

        // 计算中位行间隔
        List<Integer> gaps = new ArrayList<>();
        for (int i = 1; i < raw.size(); i++) {
            gaps.add(raw.get(i).lineNumber - raw.get(i - 1).lineNumber);
        }
        Collections.sort(gaps);
        int medianGap = gaps.get(gaps.size() / 2);

        // 中位间隔 < 5 行 → 大概率是正文数字被误识别，自动提高门限
        int minGap = 5;
        if (medianGap < minGap) {
            int threshold = Math.max(medianGap * 3, minGap);
            List<ChapterInfo> filtered = new ArrayList<>();
            filtered.add(raw.get(0));
            for (int i = 1; i < raw.size(); i++) {
                if (raw.get(i).lineNumber - filtered.get(filtered.size() - 1).lineNumber >= threshold) {
                    filtered.add(raw.get(i));
                }
            }
            return filtered;
        }
        return raw;
    }

    // ==================== 数据类 ====================

    private record ChapterInfo(String title, int lineNumber, int charOffset, String format) {}
}
