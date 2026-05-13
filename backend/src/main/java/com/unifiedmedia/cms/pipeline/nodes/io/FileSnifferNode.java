package com.unifiedmedia.cms.pipeline.nodes.io;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.FileCandidate;
import com.unifiedmedia.cms.pipeline.payload.PipelineKeys;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@Slf4j
public class FileSnifferNode extends BaseInputNode {

    public FileSnifferNode() {
        super("FileSnifferNode", "文件嗅探", "🔍",
                "扫描源目录，探测所有文件 MIME 类型，产生 FileCandidate 列表。不污染全局上下文。",
                List.of(),
                List.of());
    }

    @Override
    public boolean canExecute(TaskContext context) {
        return context.getPipelineData(PipelineKeys.SOURCE_DIRECTORY, String.class) != null
                || context.getPipelineData(PipelineKeys.RELATIVE_PATH, String.class) != null
                || context.getPipelineData(PipelineKeys.ABSOLUTE_PATH, String.class) != null;
    }

    @Override
    public void execute(TaskContext context) throws IOException {
        String sourceDir = context.getPipelineData(PipelineKeys.SOURCE_DIRECTORY, String.class);
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
        List<FileCandidate> candidates = new ArrayList<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String mimeType = probeMimeType(file);
                String format = mimeToFormat(mimeType, file.toString());
                candidates.add(new FileCandidate(
                        file.toAbsolutePath().toString(),
                        dir.relativize(file).toString(),
                        mimeType,
                        format));
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                context.addLog("err", "Failed to access: " + file + " - " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
        context.setPipelineData(PipelineKeys.FILE_CANDIDATES, candidates);
        if (candidates.isEmpty()) {
            context.addLog("warn", "No files found in source directory: " + sourceDir);
        }
        context.addLog("ok", "扫描完成: 发现 " + candidates.size() + " 个文件");
        log.info("[FileSnifferNode] Scanned directory: {}, found {} files, first format={}",
                sourceDir, candidates.size(), candidates.isEmpty() ? "N/A" : candidates.get(0).format());
    }

    private void sniffSingleFile(TaskContext context) throws IOException {
        String path = context.getPipelineData(PipelineKeys.ABSOLUTE_PATH, String.class);
        if (path == null) path = context.getPipelineData(PipelineKeys.RELATIVE_PATH, String.class);
        Path filePath = Path.of(path);
        if (!Files.exists(filePath)) throw new IOException("File not found: " + path);
        String mimeType = probeMimeType(filePath);
        String format = mimeToFormat(mimeType, path);
        List<FileCandidate> candidates = List.of(new FileCandidate(
                filePath.toAbsolutePath().toString(),
                filePath.getFileName().toString(),
                mimeType, format));
        context.setPipelineData(PipelineKeys.FILE_CANDIDATES, candidates);
        context.addLog("ok", "文件嗅探: MIME=" + mimeType + ", 格式=" + format);
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
