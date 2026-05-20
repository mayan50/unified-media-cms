package com.unifiedmedia.cms.service;

import com.unifiedmedia.cms.entity.StorageNode;
import com.unifiedmedia.cms.pipeline.core.StorageAdapter;
import com.unifiedmedia.cms.pipeline.core.VfsFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地磁盘存储适配器 — 零拷贝优化。
 * <p>
 * 本地文件无需下载到沙箱，直接返回原始路径供下游只读使用。
 */
@Slf4j
@Component
public class LocalStorageAdapter implements StorageAdapter {

    @Override
    public String supports() { return "LOCAL"; }

    @Override
    public List<VfsFile> listFiles(StorageNode node, String targetPath) {
        Path root = resolveBasePath(node);
        Path dir = targetPath != null && !targetPath.isBlank() ? root.resolve(targetPath) : root;
        if (!Files.isDirectory(dir)) return List.of();

        List<VfsFile> entries = new ArrayList<>();
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    entries.add(VfsFile.builder()
                            .storageNodeId(node.getId())
                            .providerType("LOCAL")
                            .remotePath(file.toAbsolutePath().toString())
                            .fileName(dir.relativize(file).toString())
                            .size(attrs.size())
                            .isDirectory(false)
                            .creationTime(attrs.creationTime().toMillis())
                            .lastModifiedTime(attrs.lastModifiedTime().toMillis())
                            .build());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!root.equals(dir)) {
                        entries.add(VfsFile.builder()
                                .storageNodeId(node.getId())
                                .providerType("LOCAL")
                                .remotePath(dir.toAbsolutePath().toString())
                                .fileName(root.relativize(dir).toString())
                                .isDirectory(true)
                                .build());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("[LocalStorageAdapter] Failed to list files in {}", dir, e);
        }
        return entries;
    }

    @Override
    public Path downloadToSandbox(StorageNode node, VfsFile file, String taskId) {
        // 零拷贝：本地文件直接返回原始路径
        return Path.of(file.getRemotePath());
    }

    @Override
    public void uploadFromSandbox(StorageNode node, Path localSandboxFile, String targetRemotePath) {
        // 本地文件无需上传，已在原位
    }

    private Path resolveBasePath(StorageNode node) {
        return Path.of(node.getConnectionConfig() != null ? node.getConnectionConfig() : "/");
    }
}
