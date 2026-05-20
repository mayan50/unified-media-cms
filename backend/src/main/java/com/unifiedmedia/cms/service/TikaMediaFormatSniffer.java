package com.unifiedmedia.cms.service;

import com.unifiedmedia.cms.pipeline.core.MediaFormat;
import com.unifiedmedia.cms.pipeline.core.MediaFormatSniffer;
import com.unifiedmedia.cms.pipeline.core.StorageAdapter;
import com.unifiedmedia.cms.pipeline.core.VfsFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 基于文件扩展名和 JDK 内置探测的简单嗅探器。
 * <p>
 * 未来可替换为 Apache Tika 实现深度 Magic Number 嗅探。
 */
@Slf4j
@Component
public class TikaMediaFormatSniffer implements MediaFormatSniffer {

    @Override
    public String probeMimeType(VfsFile file, StorageAdapter adapter) {
        // 1. 尝试通过 JDK 内置 Files.probeContentType 探测
        if ("LOCAL".equals(file.getProviderType())) {
            try {
                String mime = Files.probeContentType(Path.of(file.getRemotePath()));
                if (mime != null && !mime.isBlank()) return mime;
            } catch (IOException ignored) {}
        }

        // 2. 回退：通过文件扩展名匹配 MediaFormat 枚举中的 mimeType
        String fileName = file.getFileName() != null ? file.getFileName() : file.getRemotePath();
        if (fileName != null && fileName.contains(".")) {
            String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
            MediaFormat fmt = MediaFormat.fromExtension(ext);
            if (fmt != MediaFormat.UNKNOWN) return fmt.getMimeType();
        }

        return "application/octet-stream";
    }
}
