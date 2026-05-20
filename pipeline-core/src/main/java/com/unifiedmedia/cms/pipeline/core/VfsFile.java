package com.unifiedmedia.cms.pipeline.core;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * 虚拟文件系统数据传输对象 — 流水线中统一使用的文件模型。
 * <p>
 * 无论文件在本地磁盘、云盘还是对象存储，均以此模型在节点间流转。
 * 替代旧的 {@code FileCandidate} record。
 */
@Data
@Builder
public class VfsFile {

    // ── 身份标识 ──
    private UUID storageNodeId;
    private String providerType;

    /** 文件在存储介质中的路径（本地为绝对路径，远端为相对路径） */
    private String remotePath;
    private String fileName;

    // ── 强类型属性 ──
    private String mimeType;
    /** 格式枚举，由 MediaFormatSniffer 深度嗅探后赋值 */
    private MediaFormat format;

    // ── 物理属性 ──
    private Long size;
    private boolean isDirectory;
    private Long creationTime;
    private Long lastModifiedTime;

    // ── 兼容方法（旧 FileCandidate 迁移过渡）──

    /** @deprecated 使用 {@link #getRemotePath()} */
    public String getAbsolutePath() { return remotePath; }

    /** @deprecated 使用 {@link #getFileName()} */
    public String getRelativePath() { return fileName; }
}
