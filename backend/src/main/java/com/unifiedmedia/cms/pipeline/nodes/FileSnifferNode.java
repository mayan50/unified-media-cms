package com.unifiedmedia.cms.pipeline.nodes;

import com.unifiedmedia.cms.pipeline.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@Slf4j
@Component
public class FileSnifferNode extends BaseInputNode {

    public FileSnifferNode() {
        super("FileSnifferNode", "文件嗅探", "🔍",
                "扫描源目录，探测所有文件 MIME 类型，创建处理文件列表。源路径由新建任务时指定",
                List.of(),
                List.of());
    }

    @Override
    public boolean canExecute(TaskContext context) {
        return context.getSourceDirectory() != null
                || context.getRelativePath() != null
                || context.getAbsolutePath() != null;
    }

    @Override
    public void execute(TaskContext context) throws IOException {
        String sourceDir = context.getSourceDirectory();
        if (sourceDir != null) {
            sniffDirectory(context, sourceDir);
        } else {
            sniffSingleFile(context);
        }
    }

    private void sniffDirectory(TaskContext context, String sourceDir) throws IOException {
        Path dir = Path.of(sourceDir);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new IOException("Source directory not found: " + sourceDir);
        }
        Map<String, String> taggedFiles = new LinkedHashMap<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String mimeType = probeMimeType(file);
                String format = mimeToFormat(mimeType, file.toString());
                taggedFiles.put(file.toAbsolutePath().toString(), mimeType);
                if (context.getDetectedFormat() == null && !"UNKNOWN".equals(format)) {
                    context.setDetectedMimeType(mimeType);
                    context.setDetectedFormat(format);
                    context.setAbsolutePath(file.toAbsolutePath().toString());
                    context.setRelativePath(dir.relativize(file).toString());
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                context.getErrors().add("Failed to access: " + file + " - " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
        context.setTaggedFiles(taggedFiles);
        if (taggedFiles.isEmpty()) {
            context.getErrors().add("No files found in source directory: " + sourceDir);
        }
        log.info("[FileSnifferNode] Scanned directory: {}, found {} files, first format={}",
                sourceDir, taggedFiles.size(), context.getDetectedFormat());
    }

    private void sniffSingleFile(TaskContext context) throws IOException {
        String path = context.getAbsolutePath();
        if (path == null) path = context.getRelativePath();
        Path filePath = Path.of(path);
        if (!Files.exists(filePath)) throw new IOException("File not found: " + path);
        String mimeType = probeMimeType(filePath);
        String format = mimeToFormat(mimeType, path);
        context.setDetectedMimeType(mimeType);
        context.setDetectedFormat(format);
        context.setTaggedFiles(Map.of(filePath.toAbsolutePath().toString(), mimeType));
        log.info("[FileSnifferNode] Single file: mimeType={}, format={}, path={}", mimeType, format, path);
    }

    private String probeMimeType(Path file) {
        try { String mt = Files.probeContentType(file); if (mt != null) return mt; } catch (IOException ignored) {}
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".txt")) return "text/plain";
        if (name.endsWith(".epub")) return "application/epub+zip";
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".mp4")) return "video/mp4";
        if (name.endsWith(".mkv")) return "video/x-matroska";
        return "application/octet-stream";
    }

    private String mimeToFormat(String mimeType, String path) {
        if (mimeType.contains("text/plain") || path.toLowerCase().endsWith(".txt")) return "TXT";
        if (mimeType.contains("epub") || path.toLowerCase().endsWith(".epub")) return "EPUB";
        if (mimeType.contains("pdf") || path.toLowerCase().endsWith(".pdf")) return "PDF";
        if (mimeType.startsWith("video/")) return "VIDEO";
        return "UNKNOWN";
    }
}
