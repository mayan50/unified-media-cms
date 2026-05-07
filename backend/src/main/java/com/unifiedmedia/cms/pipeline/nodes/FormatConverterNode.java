package com.unifiedmedia.cms.pipeline.nodes;

import com.unifiedmedia.cms.pipeline.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Component
public class FormatConverterNode extends BaseProcessingNode {

    public FormatConverterNode() {
        super("FormatConverterNode", "TXT→EPUB", "🔄",
                "将 TXT 文本转换为标准 EPUB 电子书格式",
                List.of(),
                List.of());
    }

    @Override
    public boolean canExecute(TaskContext context) {
        return "TXT".equals(context.getDetectedFormat());
    }

    @Override
    public void execute(TaskContext context) throws IOException {
        log.info("[FormatConverterNode] Converting TXT to EPUB");
        String title = context.getFinalTitle() != null ? context.getFinalTitle() :
                       context.getAiTitle() != null ? context.getAiTitle() : "Unknown Title";
        String author = context.getFinalAuthor() != null ? context.getFinalAuthor() :
                        context.getAiAuthor() != null ? context.getAiAuthor() : "Unknown Author";
        String path = context.getAbsolutePath();
        if (path == null) path = context.getRelativePath();
        String txtContent = Files.readString(Path.of(path), StandardCharsets.UTF_8);
        byte[] epubBytes = createBasicEpub(title, author, txtContent);
        context.setConvertedEpubBytes(epubBytes);
        log.info("[FormatConverterNode] EPUB generated, size: {} bytes", epubBytes.length);
    }

    private byte[] createBasicEpub(String title, String author, String content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        // mimetype
        ZipEntry mimetypeEntry = new ZipEntry("mimetype");
        mimetypeEntry.setMethod(ZipEntry.STORED);
        byte[] mtBytes = "application/epub+zip".getBytes(StandardCharsets.UTF_8);
        mimetypeEntry.setSize(mtBytes.length); mimetypeEntry.setCompressedSize(mtBytes.length);
        CRC32 crc = new CRC32(); crc.update(mtBytes); mimetypeEntry.setCrc(crc.getValue());
        zos.putNextEntry(mimetypeEntry); zos.write(mtBytes); zos.closeEntry();
        // container.xml
        writeZip(zos, "META-INF/container.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\"><rootfiles><rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/></rootfiles></container>");
        // content.opf
        writeZip(zos, "OEBPS/content.opf",
                String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?><package xmlns=\"http://www.idpf.org/2007/opf\" unique-identifier=\"BookId\" version=\"3.0\"><metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\"><dc:identifier id=\"BookId\">urn:uuid:%s</dc:identifier><dc:title>%s</dc:title><dc:creator>%s</dc:creator><dc:language>zh</dc:language></metadata><manifest><item id=\"chapter1\" href=\"chapter1.xhtml\" media-type=\"application/xhtml+xml\"/><item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/></manifest><spine><itemref idref=\"chapter1\"/></spine></package>",
                        UUID.randomUUID(), esc(title), esc(author)));
        // chapter
        writeZip(zos, "OEBPS/chapter1.xhtml",
                String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>%s</title></head><body>%s</body></html>",
                        esc(title), esc(content).replace("\n", "<br/>\n")));
        // nav
        writeZip(zos, "OEBPS/nav.xhtml",
                String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\"><head><title>TOC</title></head><body><nav epub:type=\"toc\"><ol><li><a href=\"chapter1.xhtml\">%s</a></li></ol></nav></body></html>", esc(title)));
        zos.finish(); zos.close();
        return baos.toByteArray();
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
}
