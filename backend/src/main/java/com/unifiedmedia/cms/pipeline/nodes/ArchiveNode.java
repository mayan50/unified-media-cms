package com.unifiedmedia.cms.pipeline.nodes;

import com.unifiedmedia.cms.entity.*;
import com.unifiedmedia.cms.repository.*;
import com.unifiedmedia.cms.pipeline.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ArchiveNode extends BaseOutputNode {

    private final MediaAssetRepository mediaAssetRepository;
    private final MediaFileRepository mediaFileRepository;
    private final StorageNodeRepository storageNodeRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CreatorRepository creatorRepository;
    private final BookDetailRepository bookDetailRepository;

    public ArchiveNode(MediaAssetRepository mediaAssetRepository,
                       MediaFileRepository mediaFileRepository,
                       StorageNodeRepository storageNodeRepository,
                       CategoryRepository categoryRepository,
                       TagRepository tagRepository,
                       CreatorRepository creatorRepository,
                       BookDetailRepository bookDetailRepository) {
        super("ArchiveNode", "归档写入", "📦",
                "将处理完成的文件写入目标媒体库目录并入库。输出路径由新建任务时指定",
                List.of(),
                List.of());
        this.mediaAssetRepository = mediaAssetRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.storageNodeRepository = storageNodeRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.creatorRepository = creatorRepository;
        this.bookDetailRepository = bookDetailRepository;
    }

    @Override
    public boolean canExecute(TaskContext context) {
        String title = context.getFinalTitle() != null ? context.getFinalTitle() : context.getAiTitle();
        return title != null && !title.isBlank();
    }

    @Override
    @Transactional
    public void execute(TaskContext context) throws IOException {
        log.info("[ArchiveNode] Archiving asset...");
        UUID assetId = context.getAssetId();
        MediaAsset asset = mediaAssetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalStateException("Asset not found: " + assetId));

        String title = context.getFinalTitle() != null ? context.getFinalTitle() : context.getAiTitle();
        String author = context.getFinalAuthor() != null ? context.getFinalAuthor() : context.getAiAuthor();
        String summary = context.getFinalSummary() != null ? context.getFinalSummary() : context.getAiSummary();
        String coverUrl = context.getFinalCoverUrl();
        Integer publishYear = context.getFinalPublishYear();

        asset.setTitle(title);
        asset.setSummary(summary);
        asset.setCoverUrl(coverUrl);
        asset.setPublishYear(publishYear);
        asset.setStatus("COMPLETED");
        mediaAssetRepository.save(asset);

        // Tags & creators
        List<String> tags = context.getFinalTags() != null ? context.getFinalTags() : context.getAiTags();
        if (tags != null) {
            for (String tagName : tags) {
                tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
            }
        }
        if (author != null) {
            creatorRepository.findByName(author)
                    .orElseGet(() -> creatorRepository.save(Creator.builder().name(author).build()));
        }

        // 保存书籍详情
        if ("BOOK".equals(asset.getMediaType())) {
            saveBookDetail(context, asset);
        }

        // Write EPUB if converted
        if (context.getConvertedEpubBytes() != null) {
            writeEpubFile(context, asset);
        }

        // Record original file
        if (context.getStorageNodeId() != null) {
            MediaFile originalFile = MediaFile.builder()
                    .assetId(asset.getId())
                    .storageNodeId(context.getStorageNodeId())
                    .fileFormat(context.getDetectedFormat())
                    .relativePath(context.getRelativePath())
                    .isPrimary(context.getConvertedEpubBytes() == null)
                    .build();
            mediaFileRepository.save(originalFile);
        }

        log.info("[ArchiveNode] Asset archived: id={}, title={}", asset.getId(), title);
    }

    private void writeEpubFile(TaskContext context, MediaAsset asset) throws IOException {
        StorageNode targetNode = storageNodeRepository.findById(context.getStorageNodeId())
                .orElseThrow(() -> new IOException("Storage node not found: " + context.getStorageNodeId()));
        String basePath = extractBasePath(targetNode);
        String epubFileName = Path.of(context.getRelativePath()).getFileName().toString()
                .replaceAll("\\.(txt|TXT)$", ".epub");
        Path epubPath = Path.of(basePath, epubFileName);
        Files.createDirectories(epubPath.getParent());
        Files.write(epubPath, context.getConvertedEpubBytes());
        MediaFile epubFile = MediaFile.builder()
                .assetId(asset.getId())
                .storageNodeId(targetNode.getId())
                .fileFormat("EPUB")
                .relativePath(epubFileName)
                .fileSize((long) context.getConvertedEpubBytes().length)
                .isPrimary(true)
                .build();
        mediaFileRepository.save(epubFile);
        log.info("[ArchiveNode] EPUB written: path={}, size={}", epubPath, context.getConvertedEpubBytes().length);
    }

    private void saveBookDetail(TaskContext context, MediaAsset asset) {
        BookDetail detail = BookDetail.builder()
                .assetId(asset.getId())
                .publisher(context.get("publisher") instanceof String s ? s : null)
                .language(context.get("language") instanceof String s ? s : null)
                .doubanId(context.get("doubanId") instanceof String s ? s : null)
                .isbn13(context.get("isbn13") instanceof String s ? s : null)
                .isbn10(context.get("isbn10") instanceof String s ? s : null)
                .asin(context.get("asin") instanceof String s ? s : null)
                .build();
        bookDetailRepository.save(detail);
        log.info("[ArchiveNode] Book detail saved for asset: {}", asset.getId());
    }

    private String extractBasePath(StorageNode node) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var config = mapper.readValue(node.getConnectionConfig(), java.util.Map.class);
            Object bp = config.get("basePath");
            if (bp != null) return bp.toString();
        } catch (Exception ignored) {}
        return "/tmp/unified-media-cms";
    }
}
