package com.unifiedmedia.cms.pipeline.core;

/**
 * 媒体格式嗅探器 — 识别文件的真实 MIME 类型。
 * <p>
 * 接收 {@link VfsFile} 和 {@link StorageAdapter} 作为参数，允许适配器按需拉取少量数据块
 * （如文件头 Magic Number）进行嗅探，避免为了鉴定格式而下载整个远端文件。
 */
public interface MediaFormatSniffer {

    /**
     * 探测文件的真实 MIME 类型。
     *
     * @param file    目标文件（可能尚未下载到本地）
     * @param adapter 对应的存储适配器（用于按需读取远端文件头）
     * @return MIME 类型字符串，如 "application/epub+zip"
     */
    String probeMimeType(VfsFile file, StorageAdapter adapter);
}
