package com.unifiedmedia.cms.pipeline.nodes.io;

import com.unifiedmedia.cms.entity.*;
import com.unifiedmedia.cms.pipeline.core.annotation.NodeDef;
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
@NodeDef(name = "ArchiveNode", label = "归档写入", icon = "📦", type = NodeType.OUTPUT,
        description = "将处理完成的文件写入目标媒体库目录并入库")
public class ArchiveNode extends BaseOutputNode implements EntityDataOperator {

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
        AssetDraft draft = context.getAssetDraft();
        return draft != null && draft.getTitle() != null && !draft.getTitle().isBlank();
    }

    @Override
    public void execute(TaskContext context) throws IOException {
        AssetDraft draft = context.getAssetDraft();
        if (draft == null) throw new IllegalStateException("AssetDraft not found in context");
        UUID assetId = context.getAssetId();

        // EPUB 文件写入（纯文件 I/O，不涉及数据库）
        byte[] epubBytes = context.getPipelineData(PipelineKeys.CONVERTED_EPUB, byte[].class);
        if (epubBytes != null) {
            writeEpubFile(context, draft, epubBytes);
            context.addLog("ok", "写入 EPUB 文件: " + epubBytes.length + " 字节");
        }

        // 记录原始文件（AssetFile 不属于 Draft，直接写）
        UUID storageNodeId = context.getPipelineData(PipelineKeys.STORAGE_NODE_ID, UUID.class);
        if (storageNodeId != null) {
            AssetFile originalFile = AssetFile.builder()
                    .assetId(assetId).storageNodeId(storageNodeId)
                    .fileFormat(context.getPipelineData(PipelineKeys.DETECTED_FORMAT, String.class))
                    .relativePath(context.getPipelineData(PipelineKeys.RELATIVE_PATH, String.class))
                    .isPrimary(epubBytes == null).build();
            mediaFileRepository.save(originalFile);
            context.addLog("ok", "记录原始文件");
        }

        context.addLog("ok", "归档完成: id=" + assetId + ", 标题=" + draft.getTitle());
        log.info("[ArchiveNode] Asset archived: id={}, title={}", assetId, draft.getTitle());
    }

    private void writeEpubFile(TaskContext context, AssetDraft draft, byte[] epubBytes) throws IOException {
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
                .assetId(context.getAssetId()).storageNodeId(outputNodeId)
                .fileFormat("EPUB").relativePath(epubFileName)
                .fileSize((long) epubBytes.length).isPrimary(true).build();
        mediaFileRepository.save(epubFile);
        log.info("[ArchiveNode] EPUB written: path={}, size={}", epubPath, epubBytes.length);
    }
}
