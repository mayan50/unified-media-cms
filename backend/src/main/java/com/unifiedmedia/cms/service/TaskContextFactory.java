package com.unifiedmedia.cms.service;

import com.unifiedmedia.cms.entity.*;
import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.mapper.AssetDraftMapper;
import com.unifiedmedia.cms.pipeline.mapper.MediaDetailDraftMapper;
import com.unifiedmedia.cms.pipeline.payload.PipelineKeys;
import com.unifiedmedia.cms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;

/**
 * 上下文工厂 — 负责查询数据库实体，通过 Mapper 转为纯净 Draft，组装 PipelineTaskContext。
 * <p>
 * 对象拷贝职责委托给 {@link AssetDraftMapper} 和 {@link MediaDetailDraftMapper}，
 * 此处只做"查库取数 + 组装"，不手写 setter。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskContextFactory {

    private final AssetRepository assetRepository;
    private final AssetCreatorRepository assetCreatorRepository;
    private final CreatorRepository creatorRepository;
    private final BookDetailRepository bookDetailRepository;
    private final TaskRepository taskRepository;
    private final AssetDraftMapper assetDraftMapper;
    private final MediaDetailDraftMapper mediaDetailDraftMapper;

    public PipelineTaskContext buildContext(UUID jobId, VfsFile file, Map<String, Object> globalConfigs) {
        Asset asset = findOrCreateAsset(jobId, file);
        PipelineTaskContext ctx = new PipelineTaskContext(UUID.randomUUID(), asset.getId());
        ctx.setTargetFile(file);

        // Draft 由 Mapper 生成，避免手动 setter
        ctx.setAssetDraft(assetDraftMapper.createDraftFromEntity(asset));
        bookDetailRepository.findByAssetId(asset.getId()).ifPresent(bd ->
                ctx.setDetailDraft(mediaDetailDraftMapper.createDraftFromEntity(bd)));

        // 全局配置 + 文件数据写入 data 总线
        globalConfigs.forEach(ctx::setPipelineData);
        ctx.setPipelineData(PipelineKeys.ABSOLUTE_PATH, file.getRemotePath());
        ctx.setPipelineData(PipelineKeys.RELATIVE_PATH, file.getFileName());
        ctx.setPipelineData(PipelineKeys.DETECTED_MIME_TYPE, file.getMimeType());
        ctx.setPipelineData(PipelineKeys.DETECTED_FORMAT, file.getFormat());

        // 已有作者
        List<AssetCreator> acs = assetCreatorRepository.findByAssetId(asset.getId());
        if (!acs.isEmpty()) {
            List<String> authorNames = acs.stream()
                    .filter(ac -> "作者".equals(ac.getRole()))
                    .map(ac -> creatorRepository.findById(ac.getCreatorId()).map(Creator::getName).orElse(null))
                    .filter(Objects::nonNull).toList();
            if (!authorNames.isEmpty()) ctx.setPipelineData(PipelineKeys.AUTHORS, authorNames);
        }
        if (!asset.getTags().isEmpty()) {
            ctx.setPipelineData(PipelineKeys.TAGS, asset.getTags().stream().map(Tag::getName).toList());
        }

        return ctx;
    }

    private Asset findOrCreateAsset(UUID jobId, VfsFile file) {
        List<Task> tasks = taskRepository.findByJobIdOrderByCreatedAtAsc(jobId);
        for (int i = tasks.size() - 1; i >= 0; i--) {
            if (file.getRemotePath().equals(tasks.get(i).getFilePath())) {
                Task prev = tasks.get(i);
                if (prev.getAssetId() != null) {
                    return assetRepository.findById(prev.getAssetId()).orElseGet(() -> createAsset(file));
                }
            }
        }
        return createAsset(file);
    }

    private Asset createAsset(VfsFile file) {
        Asset a = Asset.builder()
                .title(Path.of(file.getRemotePath()).getFileName().toString().replaceAll("\\.[^.]+$", ""))
                .mediaType("BOOK").build();
        return assetRepository.save(a);
    }
}
