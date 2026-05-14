package com.unifiedmedia.cms.pipeline.nodes.processing;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.*;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class FormatConverterNode extends BaseProcessingNode {

    public FormatConverterNode() {
        super("FormatConverterNode", "TXT→EPUB", "🔄",
                "将 TXT 文本转换为标准 EPUB 电子书格式，含章节目录",
                List.of(),
                List.of());
    }

    @Override
    public boolean canExecute(TaskContext context) {
        return "TXT".equals(context.getPipelineData(PipelineKeys.DETECTED_FORMAT, String.class));
    }

    @Override
    public void execute(TaskContext context) throws IOException {
        log.info("[FormatConverterNode] Converting TXT to EPUB");
        String assetTitle = context.getAsset() != null ? context.getAsset().getTitle() : null;
        String title = assetTitle != null ? assetTitle : "Unknown Title";

        // Read authors as list
        List<String> authors = context.getPipelineList(PipelineKeys.AUTHORS, String.class);
        String author = !authors.isEmpty() ? String.join(", ", authors) : "Unknown Author";

        // Read summary from asset
        String summary = context.getAsset() != null ? context.getAsset().getSummary() : "";
        if (summary != null && !summary.isBlank()) {
            title = title + (summary.length() > 50 ? " - " + summary.substring(0, 50) : "");
        }

        String path = context.getPipelineData(PipelineKeys.ABSOLUTE_PATH, String.class);
        if (path == null) path = context.getPipelineData(PipelineKeys.RELATIVE_PATH, String.class);
        String txtContent = FileUtil.readString(Path.of(path));

        // 读取章节列表
        List<ChapterInfo> chapters = context.getPipelineList(PipelineKeys.CHAPTERS, ChapterInfo.class);
        if (chapters == null) chapters = List.of();

        context.addLog("ok", "读取源文本: " + txtContent.length() + " 字符");
        byte[] epubBytes = createEpub(title, author, txtContent, chapters);
        context.setPipelineData(PipelineKeys.CONVERTED_EPUB, epubBytes);
        context.addLog("ok", "切分章节: " + chapters.size() + " 章");
        context.addLog("ok", "生成 EPUB: " + epubBytes.length + " 字节");
        context.addLog("ok", "TXT→EPUB 转换完成: 标题=" + title + ", 大小=" + (epubBytes.length / 1024) + "KB");
        log.info("[FormatConverterNode] EPUB generated, size: {} bytes, chapters: {}", epubBytes.length, chapters.size());
    }

    private byte[] createEpub(String title, String author, String content, List<ChapterInfo> chapters) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = getZipOutputStream(baos);

        // container.xml
        writeZip(zos, "META-INF/container.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\"><rootfiles><rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/></rootfiles></container>");

        // 切分章节内容
        List<ChapterPart> parts = splitChapters(content, chapters);

        // 构建 manifest 和 spine
        StringBuilder manifest = new StringBuilder();
        StringBuilder spine = new StringBuilder();
        StringBuilder nav = new StringBuilder("<nav epub:type=\"toc\"><h1>目录</h1><ol>");

        for (int i = 0; i < parts.size(); i++) {
            String id = "chapter" + (i + 1);
            String file = id + ".xhtml";
            manifest.append(String.format("<item id=\"%s\" href=\"%s\" media-type=\"application/xhtml+xml\"/>", id, file));
            spine.append(String.format("<itemref idref=\"%s\"/>", id));
            nav.append(String.format("<li><a href=\"%s\">%s</a></li>", file, esc(parts.get(i).title)));

            // 写章节文件
            String body = esc(parts.get(i).body).replace("\n", "<br/>\n");
            String chapterHtml = String.format(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>%s</title></head><body><h2>%s</h2>%s</body></html>",
                    esc(parts.get(i).title), esc(parts.get(i).title), body);
            writeZip(zos, "OEBPS/" + file, chapterHtml);
        }
        nav.append("</ol></nav>");

        manifest.append("<item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>");

        // content.opf
        writeZip(zos, "OEBPS/content.opf",
                String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?><package xmlns=\"http://www.idpf.org/2007/opf\" unique-identifier=\"BookId\" version=\"3.0\"><metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\"><dc:identifier id=\"BookId\">urn:uuid:%s</dc:identifier><dc:title>%s</dc:title><dc:creator>%s</dc:creator><dc:language>zh</dc:language></metadata><manifest>%s</manifest><spine>%s</spine></package>",
                        UUID.randomUUID(), esc(title), esc(author), manifest, spine));

        // nav.xhtml
        writeZip(zos, "OEBPS/nav.xhtml",
                String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\"><head><title>TOC</title></head><body>%s</body></html>", nav));

        zos.finish(); zos.close();
        return baos.toByteArray();
    }

    private static ZipOutputStream getZipOutputStream(ByteArrayOutputStream baos) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(baos);

        // mimetype
        ZipEntry mimetypeEntry = new ZipEntry("mimetype");
        mimetypeEntry.setMethod(ZipEntry.STORED);
        byte[] mtBytes = "application/epub+zip".getBytes(StandardCharsets.UTF_8);
        mimetypeEntry.setSize(mtBytes.length);
        mimetypeEntry.setCompressedSize(mtBytes.length);
        CRC32 crc = new CRC32();
        crc.update(mtBytes);
        mimetypeEntry.setCrc(crc.getValue());
        zos.putNextEntry(mimetypeEntry);
        zos.write(mtBytes);
        zos.closeEntry();
        return zos;
    }

    private List<ChapterPart> splitChapters(String content, List<ChapterInfo> chapters) {
        List<ChapterPart> parts = new ArrayList<>();
        String[] lines = content.split("\\R");

        if (chapters.isEmpty()) {
            parts.add(new ChapterPart("正文", content));
            return parts;
        }

        // 按章节切分
        for (int i = 0; i < chapters.size(); i++) {
            ChapterInfo ch = chapters.get(i);
            int startLine = ch.lineNumber() - 1; // 0-indexed
            int endLine = (i + 1 < chapters.size())
                    ? chapters.get(i + 1).lineNumber() - 1
                    : lines.length;

            String chTitle = ch.title();
            StringBuilder body = new StringBuilder();
            for (int j = startLine; j < Math.min(endLine, lines.length); j++) {
                body.append(lines[j]).append('\n');
            }
            parts.add(new ChapterPart(chTitle != null ? chTitle : "第" + (i + 1) + "章", body.toString().trim()));
        }

        // 如果第一章之前有内容，加到第一章前面
        if (!parts.isEmpty()) {
            int firstLine = chapters.get(0).lineNumber() - 1;
            if (firstLine > 0) {
                StringBuilder pre = new StringBuilder();
                for (int j = 0; j < firstLine; j++) pre.append(lines[j]).append('\n');
                parts.get(0).body = pre.toString().trim() + "\n\n" + parts.get(0).body;
            }
        }

        return parts;
    }

    private void writeZip(ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static class ChapterPart {
        final String title;
        String body;
        ChapterPart(String t, String b) { title = t; body = b; }
    }
}
