package com.unifiedmedia.cms.pipeline.nodes.io;

import com.unifiedmedia.cms.entity.*;
import com.unifiedmedia.cms.repository.*;
import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.PipelineKeys;
import com.unifiedmedia.cms.pipeline.spi.RelationMerger;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public class ArchiveNode extends BaseOutputNode {

    private final AssetRepository mediaAssetRepository;
    private final AssetFileRepository mediaFileRepository;
    private final BookDetailRepository bookDetailRepository;
    private final List<RelationMerger> relationMergers;

    public ArchiveNode(AssetRepository mediaAssetRepository,
                       AssetFileRepository mediaFileRepository,
                       BookDetailRepository bookDetailRepository,
                       List<RelationMerger> relationMergers) {
        super("ArchiveNode", "归档写入", "📦",
                "将处理完成的文件写入目标媒体库目录并入库。",
                List.of(), List.of());
        this.mediaAssetRepository = mediaAssetRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.bookDetailRepository = bookDetailRepository;
        this.relationMergers = relationMergers;
    }

    @Override
    public boolean canExecute(TaskContext context) {
        Asset asset = context.getAsset();
        return asset != null && asset.getTitle() != null && !asset.getTitle().isBlank();
    }

    @Override
    public void execute(TaskContext context) throws IOException {
        Asset asset = context.getAsset();
        if (asset == null) throw new IllegalStateException("Asset not found in context");

        for (RelationMerger merger : relationMergers) {
            merger.merge(context, asset);
        }

        mediaAssetRepository.save(asset);
        context.addLog("ok", "保存资产: " + asset.getId());

        if ("BOOK".equals(asset.getMediaType())) {
            BookDetail detail = context.getDetail(BookDetail.class);
            if (detail != null) {
                if (detail.getAssetId() == null) detail.setAssetId(asset.getId());
                bookDetailRepository.save(detail);
                context.addLog("ok", "保存书籍详情");
            }
        }

        byte[] epubBytes = context.getPipelineData(PipelineKeys.CONVERTED_EPUB, byte[].class);
        if (epubBytes != null) {
            writeEpubFile(context, asset, epubBytes);
            context.addLog("ok", "写入 EPUB 文件: " + epubBytes.length + " 字节");
        }

        UUID storageNodeId = context.getPipelineData(PipelineKeys.STORAGE_NODE_ID, UUID.class);
        if (storageNodeId != null) {
            AssetFile originalFile = AssetFile.builder()
                    .assetId(asset.getId()).storageNodeId(storageNodeId)
                    .fileFormat(context.getPipelineData(PipelineKeys.DETECTED_FORMAT, String.class))
                    .relativePath(context.getPipelineData(PipelineKeys.RELATIVE_PATH, String.class))
                    .isPrimary(epubBytes == null).build();
            mediaFileRepository.save(originalFile);
            context.addLog("ok", "记录原始文件");
        }

        context.addLog("ok", "归档完成: id=" + asset.getId() + ", 标题=" + asset.getTitle());
        log.info("[ArchiveNode] Asset archived: id={}, title={}", asset.getId(), asset.getTitle());
    }

    private void writeEpubFile(TaskContext context, Asset asset, byte[] epubBytes) throws IOException {
        String outputDir = "/tmp/unified-media-cms/output";
        UUID outputNodeId = null;
        Object targetPath = context.getPipelineData(PipelineKeys.TARGET_PATH, Object.class);
        if (targetPath instanceof Map<?, ?> tp) {
            Object p = tp.get("path");
            if (p != null) outputDir = p.toString();
            Object sid = tp.get("storage_node_id");
            if (sid != null && !sid.toString().isBlank()) outputNodeId = UUID.fromString(sid.toString());
        }
        String epubFileName = Path.of(
                (String) context.getPipelineData(PipelineKeys.RELATIVE_PATH, String.class))
                .getFileName().toString().replaceAll("\\.(txt|TXT)$", ".epub");
        Path epubPath = Path.of(outputDir, epubFileName);
        Files.createDirectories(epubPath.getParent());
        Files.write(epubPath, epubBytes);
        AssetFile epubFile = AssetFile.builder()
                .assetId(asset.getId()).storageNodeId(outputNodeId)
                .fileFormat("EPUB").relativePath(epubFileName)
                .fileSize((long) epubBytes.length).isPrimary(true).build();
        mediaFileRepository.save(epubFile);
        log.info("[ArchiveNode] EPUB written: path={}, size={}", epubPath, epubBytes.length);
    }
}
