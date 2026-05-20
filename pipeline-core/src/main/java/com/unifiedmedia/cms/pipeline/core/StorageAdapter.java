package com.unifiedmedia.cms.pipeline.core;

import com.unifiedmedia.cms.entity.StorageNode;

import java.nio.file.Path;
import java.util.List;

/**
 * 存储适配器 SPI — 虚拟文件系统的核心抽象。
 * <p>
 * 所有存储类型必须实现此接口，底座通过 {@code StorageAdapterRegistry} 自动发现。
 * 本地文件实现零拷贝优化，远端存储按需拉取。
 */
public interface StorageAdapter {

    /** 支持的存储类型，如 "LOCAL"、"WEBDAV"、"OSS" */
    String supports();

    /** 列出存储节点指定路径下的所有文件 */
    List<VfsFile> listFiles(StorageNode node, String targetPath);

    /** 将远端文件拉取到本地沙箱，返回本地路径。本地文件直接返回原始路径（零拷贝） */
    Path downloadToSandbox(StorageNode node, VfsFile file, String taskId);

    /** 将本地沙箱文件推送到远端存储 */
    void uploadFromSandbox(StorageNode node, Path localSandboxFile, String targetRemotePath);

    /** 删除远端文件（可选） */
    default void delete(StorageNode node, String remotePath) {
        throw new UnsupportedOperationException("delete not supported by " + supports());
    }

    /** 重命名远端文件（可选） */
    default void rename(StorageNode node, String oldPath, String newName) {
        throw new UnsupportedOperationException("rename not supported by " + supports());
    }
}
