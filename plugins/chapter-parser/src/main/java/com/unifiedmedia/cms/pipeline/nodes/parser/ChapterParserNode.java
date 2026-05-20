package com.unifiedmedia.cms.pipeline.nodes.parser;

import com.unifiedmedia.cms.pipeline.core.annotation.NodeDef;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
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
@NodeDef(name = "ChapterParserNode", label = "章节解析", icon = "📑", type = NodeType.PROCESSING)
public class ChapterParserNode extends BaseProcessingNode implements EntityDataOperator {

    public ChapterParserNode() {
        super("ChapterParserNode", "章节解析", "📑",
                "从 TXT 文本中智能提取书名、作者、简介和完整章节目录结构",
                List.of(
                        ConfigFieldDef.textarea("customPattern", "自定义正则", "可选：自定义章节匹配正则表达式，留空则使用内置规则")
                ),
                List.of());
    }

    // ---- 作者 ----
    private static final Pattern AUTHOR_PATTERN = Pattern.compile("(?:作者|Author|著)[：:\\s]+(.{1,40})", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUTHOR_ALT_PATTERN = Pattern.compile("^(.{2,8})\\s*著\\s*$", Pattern.MULTILINE);

    // ---- 章节目录（拆成独立 pattern 列表，避免巨型正则转义问题） ----
    private static final Pattern[] CHAPTER_PATTERNS = {
            // 第N章/回/部/卷/集/节（正文同行）
            Pattern.compile("^第\\s*\\d+\\s*[章回部卷集节][\\s：:].*"),
            Pattern.compile("^第\\s*[零一二三四五六七八九十百千万]+\\s*[章回部卷集节][\\s：:].*"),
            // 章节标记独占一行（正文在下一行）
            Pattern.compile("^第\\s*\\d+\\s*[章回部卷集节]\\s*$"),
            Pattern.compile("^第\\s*[零一二三四五六七八九十百千万]+\\s*[章回部卷集节]\\s*$"),
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
        String format = context.getPipelineData(PipelineKeys.DETECTED_FORMAT, String.class);
        return "TXT".equals(format) || "text/plain".equals(context.getPipelineData(PipelineKeys.DETECTED_MIME_TYPE, String.class));
    }

    @Override
    public void execute(TaskContext context) throws IOException {
        String path = context.getPipelineData(PipelineKeys.ABSOLUTE_PATH, String.class);
        if (path == null) path = context.getPipelineData(PipelineKeys.RELATIVE_PATH, String.class);

        String content = FileUtil.readString(Path.of(path));
        String[] lines = content.split("\\R");

        log.info("[ChapterParserNode] Parsing file: {} lines", lines.length);

        // 1. 先找章节目录，确定第一章的位置
        Pattern customPat = buildCustomPattern(context);
        List<ChapterInfo> chapters = extractChapters(lines, customPat);

        // 2. 第一章之前的内容 = 简介
        int firstChapterLine = chapters.isEmpty() ? lines.length : chapters.get(0).lineNumber() - 1;
        String introText = String.join("\n", java.util.Arrays.copyOf(lines, Math.min(firstChapterLine, lines.length))).trim();

        // 3. 从文件名解析书名和作者
        var parsed = parseFilename(Path.of(path).getFileName().toString());
        String title = parsed.title;
        String author = parsed.author;

        // 如果文件名无意义，尝试从文件内容提取
        if (isMeaninglessFilename(title)) {
            String introTitle = extractTitleFromIntro(introText);
            if (introTitle != null) {
                title = introTitle;
                log.info("[ChapterParserNode] Fallback title from intro: {}", title);
            }
        }

        if (title != null) {
            context.getAssetDraft().setTitle(title);
            log.info("[ChapterParserNode] Found title: {}", title);
        }

        // 4. 作者：文件名优先，否则从简介提取
        if (author == null) {
            author = extractAuthorFromIntro(introText);
        }
        if (author != null && !author.isBlank()) {
            setIntentIfUnlocked(context, "authors", PipelineKeys.AUTHORS, List.of(author));
            log.info("[ChapterParserNode] Found author: {}", author);
        }

        // 5. 简介作为摘要
        if (!introText.isBlank()) {
            String summary = introText.length() > 2000 ? introText.substring(0, 2000) : introText;
            context.getAssetDraft().setSummary(summary);
            log.info("[ChapterParserNode] Extracted intro: {} chars", summary.length());
        }

        // 6. 输出章节列表
        context.setPipelineData(PipelineKeys.CHAPTERS, chapters);
        context.setPipelineData(PipelineKeys.CHAPTER_COUNT, chapters.size());
        context.setPipelineData(PipelineKeys.CHAPTER_TITLES, chapters.stream().map(ChapterInfo::title).collect(Collectors.toList()));

        context.addLog("ok", "读取文件: " + lines.length + " 行");
        context.addLog(chapters.isEmpty() ? "warn" : "ok", "识别到 " + chapters.size() + " 个章节");
        if (title != null) context.addLog("ok", "提取书名: " + title);
        else context.addLog("warn", "未提取到书名");
        if (author != null) context.addLog("ok", "提取作者: " + author);
        else context.addLog("warn", "未提取到作者");
        if (!introText.isBlank()) {
            String preview = introText.length() > 80 ? introText.substring(0, 80) + "..." : introText;
            context.addLog("ok", "提取简介: " + introText.length() + " 字符 (前" + Math.min(80, introText.length()) + "字: " + preview + ")");
        } else context.addLog("warn", "未提取到简介");
        context.addLog("ok", "章节解析完成: " + chapters.size() + "章");
        log.info("[ChapterParserNode] Found {} chapters, intro={} chars, title={}", chapters.size(), introText.length(), title);
    }

    // ==================== 书名/作者提取（文件名优先） ====================

    private record ParsedFilename(String title, String author) {}

    /** 书名-作者 / 书名(作者) / 书名｜作者 / 书名_作者_XXX 等 */
    private ParsedFilename parseFilename(String fileName) {
        // 去扩展名
        String base = fileName.replaceAll("\\.[^.]+$", "").trim();
        if (base.isBlank()) return new ParsedFilename(null, null);

        // 去尾缀标记：完结、全本、精校、无广告 等
        base = base.replaceAll("[（(\\[【][^)）\\]】]*?(?:完结|全本|精校|无广告|TXT|txt|校对|未删减)[)）\\]】]?", "");
        base = base.replaceAll("[【\\[]?(?:完结|全本|精校)[】\\]]?", "");
        base = base.replaceAll("\\s+", " ").trim();
        if (base.isBlank()) return new ParsedFilename(null, null);

        // 1. 书名_作者_字数 / 书名_作者
        if (base.contains("_")) {
            String[] parts = base.split("_");
            if (parts.length >= 2) {
                String t = parts[0].trim();
                String a = parts[1].trim();
                // 第三段如果全是数字就当字数忽略
                if (a.matches("\\d+万?") || a.matches("\\d+[千百万]?")) {
                    // 第二部分是字数，不是作者
                } else if (!a.isBlank() && !a.matches("\\d+")) {
                    return new ParsedFilename(unifyQuiet(t, parts[0]), a);
                }
                return new ParsedFilename(unifyQuiet(t, parts[0]), null);
            }
        }

        // 2. 书名｜作者 或 书名|作者
        Matcher mPipe = Pattern.compile("^(.+?)\\s*[|｜]\\s*(.+)$").matcher(base);
        if (mPipe.matches()) {
            return new ParsedFilename(unifyQuiet(mPipe.group(1), mPipe.group(1)), mPipe.group(2).trim());
        }

        // 3. 书名(作者) 或 书名（作者）
        Matcher mParen = Pattern.compile("^(.+?)\\s*[（(]([^)）]+)[)）]$").matcher(base);
        if (mParen.matches()) {
            String content = mParen.group(2).trim();
            // 排除 完结/全本/上/中/下 等非作者标记
            if (!content.matches("(?i)完结|全本|精校|上|中|下|全一册|全\\d+册")) {
                return new ParsedFilename(unifyQuiet(mParen.group(1), mParen.group(1)), content);
            }
        }

        // 4. 书名 - 作者 或 书名—作者 或 书名–作者
        Matcher mDash = Pattern.compile("^(.+?)\\s*[-—–]\\s*([^-—–]+)$").matcher(base);
        if (mDash.matches()) {
            String a = mDash.group(2).trim();
            // 排除后缀不是作者的常见模式
            if (!a.matches("(?i)番外|续集|外传|前传|第一季|第二季|上|下|完结|全本|精校|TXT|txt") && a.length() <= 10) {
                return new ParsedFilename(unifyQuiet(mDash.group(1), mDash.group(1)), a);
            }
        }

        // 5. 书名【完结】等其他后缀已在上方处理
        // 纯文件名，尝试猜测：如果中文+英文混合，前面可能是书名
        if (base.matches(".*[\\u4e00-\\u9fff].*")) {
            return new ParsedFilename(base.trim(), null);
        }

        return new ParsedFilename(null, null);
    }

    /** 安静模式：保留原始部分用于书名 */
    private String unifyQuiet(String cleaned, String original) {
        String c = cleaned.trim();
        if (c.isBlank()) return null;
        return c;
    }

    /** 判断文件名是否无意义 */
    private boolean isMeaninglessFilename(String title) {
        if (title == null || title.isBlank()) return true;
        // 纯数字/字母或过短
        if (title.matches("^[a-zA-Z0-9_\\-.]+$") && title.length() <= 10) return true;
        // 随机字符串特征
        if (title.matches("^[a-f0-9]{8,}$")) return true;
        return false;
    }

    /** 从简介文本提取书名：《xxx》 */
    private String extractTitleFromIntro(String introText) {
        if (introText == null || introText.isBlank()) return null;
        Matcher m = Pattern.compile("[《「『](.+?)[》」』]").matcher(introText);
        return m.find() ? m.group(1).trim() : null;
    }

    // ==================== 作者提取（从简介文本中） ====================

    private String extractAuthorFromIntro(String introText) {
        Matcher m1 = AUTHOR_PATTERN.matcher(introText);
        if (m1.find()) return cleanAuthor(m1.group(1));
        Matcher m2 = AUTHOR_ALT_PATTERN.matcher(introText);
        if (m2.find()) return cleanAuthor(m2.group(1));
        return null;
    }

    private String cleanAuthor(String raw) {
        return raw.replaceAll("[：:\\s]", "").trim();
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
                chapters.add(new ChapterInfo(
                        line.replaceAll("\\s+", " ").trim(),
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
        String patternStr = context.getPipelineData(PipelineKeys.CUSTOM_PATTERN, String.class);
        if (patternStr != null && !patternStr.isBlank()) {
            try { return Pattern.compile(patternStr.trim(), Pattern.MULTILINE); }
            catch (Exception e) { log.warn("[ChapterParserNode] Invalid custom regex: {}", e.getMessage()); }
        }
        return null;
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
            gaps.add(raw.get(i).lineNumber() - raw.get(i - 1).lineNumber());
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
                if (raw.get(i).lineNumber() - filtered.get(filtered.size() - 1).lineNumber() >= threshold) {
                    filtered.add(raw.get(i));
                }
            }
            return filtered;
        }
        return raw;
    }

}
