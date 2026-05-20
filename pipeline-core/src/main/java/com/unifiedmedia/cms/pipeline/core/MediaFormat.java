package com.unifiedmedia.cms.pipeline.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 全景媒体格式字典 — 涵盖主流多媒体、文档及容器格式。
 * <p>
 * 通过 MIME Type 与 {@link MediaCategory} 强绑定，提供 {@code fromMimeType} 工厂方法。
 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum MediaFormat {

    // ====== 文本、电子书与漫画 (BOOK) ======
    TXT("TXT", "text/plain", MediaCategory.BOOK),
    EPUB("EPUB", "application/epub+zip", MediaCategory.BOOK),
    MOBI("MOBI", "application/x-mobipocket-ebook", MediaCategory.BOOK),
    AZW3("AZW3", "application/vnd.amazon.mobi8-ebook", MediaCategory.BOOK),
    CBZ("CBZ", "application/x-cbz", MediaCategory.BOOK),
    CBR("CBR", "application/x-cbr", MediaCategory.BOOK),

    // ====== 办公文档 (DOCUMENT) ======
    PDF("PDF", "application/pdf", MediaCategory.DOCUMENT),
    MD("MD", "text/markdown", MediaCategory.DOCUMENT),
    DOCX("DOCX", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", MediaCategory.DOCUMENT),
    XLSX("XLSX", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", MediaCategory.DOCUMENT),
    PPTX("PPTX", "application/vnd.openxmlformats-officedocument.presentationml.presentation", MediaCategory.DOCUMENT),
    DOC("DOC", "application/msword", MediaCategory.DOCUMENT),
    XLS("XLS", "application/vnd.ms-excel", MediaCategory.DOCUMENT),
    PPT("PPT", "application/vnd.ms-powerpoint", MediaCategory.DOCUMENT),

    // ====== 视频 (VIDEO) ======
    MP4("MP4", "video/mp4", MediaCategory.VIDEO),
    MKV("MKV", "video/x-matroska", MediaCategory.VIDEO),
    AVI("AVI", "video/x-msvideo", MediaCategory.VIDEO),
    MOV("MOV", "video/quicktime", MediaCategory.VIDEO),
    WEBM("WEBM", "video/webm", MediaCategory.VIDEO),
    FLV("FLV", "video/x-flv", MediaCategory.VIDEO),
    WMV("WMV", "video/x-ms-wmv", MediaCategory.VIDEO),
    RMVB("RMVB", "application/vnd.rn-realmedia-vbr", MediaCategory.VIDEO),

    // ====== 音频 (AUDIO) ======
    MP3("MP3", "audio/mpeg", MediaCategory.AUDIO),
    FLAC("FLAC", "audio/flac", MediaCategory.AUDIO),
    AAC("AAC", "audio/aac", MediaCategory.AUDIO),
    WAV("WAV", "audio/wav", MediaCategory.AUDIO),
    OGG("OGG", "audio/ogg", MediaCategory.AUDIO),
    M4A("M4A", "audio/x-m4a", MediaCategory.AUDIO),
    APE("APE", "audio/ape", MediaCategory.AUDIO),

    // ====== 图片 (IMAGE) ======
    JPG("JPG", "image/jpeg", MediaCategory.IMAGE),
    PNG("PNG", "image/png", MediaCategory.IMAGE),
    GIF("GIF", "image/gif", MediaCategory.IMAGE),
    WEBP("WEBP", "image/webp", MediaCategory.IMAGE),
    TIFF("TIFF", "image/tiff", MediaCategory.IMAGE),
    SVG("SVG", "image/svg+xml", MediaCategory.IMAGE),
    BMP("BMP", "image/bmp", MediaCategory.IMAGE),

    // ====== 压缩包、容器与镜像 (ARCHIVE) ======
    ZIP("ZIP", "application/zip", MediaCategory.ARCHIVE),
    RAR("RAR", "application/x-rar-compressed", MediaCategory.ARCHIVE),
    ISO("ISO", "application/x-iso9660-image", MediaCategory.ARCHIVE),
    SEVEN_Z("7Z", "application/x-7z-compressed", MediaCategory.ARCHIVE),
    TAR("TAR", "application/x-tar", MediaCategory.ARCHIVE),
    GZ("GZ", "application/gzip", MediaCategory.ARCHIVE),

    // ====== 可执行程序 (EXECUTABLE) ======
    EXE("EXE", "application/x-msdownload", MediaCategory.EXECUTABLE),
    APK("APK", "application/vnd.android.package-archive", MediaCategory.EXECUTABLE),
    DMG("DMG", "application/x-apple-diskimage", MediaCategory.EXECUTABLE),

    // ====== 兜底 ======
    UNKNOWN("UNKNOWN", "application/octet-stream", MediaCategory.UNKNOWN);

    private final String extension;
    private final String mimeType;
    private final MediaCategory category;

    public static MediaFormat fromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) return UNKNOWN;
        return Arrays.stream(values())
                .filter(f -> f.getMimeType().equalsIgnoreCase(mimeType))
                .findFirst().orElse(UNKNOWN);
    }

    public static MediaFormat fromExtension(String extension) {
        if (extension == null || extension.isBlank()) return UNKNOWN;
        String ext = extension.replace(".", "").toUpperCase();
        return Arrays.stream(values())
                .filter(f -> f.getExtension().equalsIgnoreCase(ext))
                .findFirst().orElse(UNKNOWN);
    }
}
