package com.unifiedmedia.cms.pipeline.core;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public final class FileUtil {

    /** 尝试顺序：UTF-8 → GBK → GB2312 → 系统默认 */
    private static final List<Charset> CHARSET_ORDER = List.of(
            StandardCharsets.UTF_8,
            Charset.forName("GBK"),
            Charset.forName("GB2312"),
            Charset.defaultCharset()
    );

    public static String readString(Path path) throws IOException {
        for (Charset cs : CHARSET_ORDER) {
            try {
                String content = Files.readString(path, cs);
                if (!content.isEmpty()) {
                    log.debug("[FileUtil] Read {} with charset {}", path.getFileName(), cs.name());
                    return content;
                }
            } catch (IOException ignored) {
                // try next charset
            }
        }
        // 最后兜底：用 ISO-8859-1 读（不会抛异常）
        try {
            return Files.readString(path, StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new IOException("Cannot read file: " + path, e);
        }
    }

    public static byte[] readAllBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }
}
