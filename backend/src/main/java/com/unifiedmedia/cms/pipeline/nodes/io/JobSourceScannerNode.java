package com.unifiedmedia.cms.pipeline.nodes.io;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.core.annotation.NodeDef;
import com.unifiedmedia.cms.pipeline.payload.PipelineKeys;
import com.unifiedmedia.cms.service.StorageAdapterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Job 级源目录扫描器 — 系统隐式节点。
 * <p>
 * 在 Job 启动时作为首个节点运行，调用 {@link StorageAdapter} 列出所有源文件，
 * 经 {@link MediaFormatSniffer} 深度嗅探后，产出 {@code List<VfsFile>} 候选列表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@NodeDef(name = "JobSourceScannerNode", label = "源目录扫描", icon = "🔍",
        type = NodeType.SYSTEM, hiddenFromUI = true)
public class JobSourceScannerNode implements PipelineNode {

    private final StorageAdapterRegistry adapterRegistry;
    private final MediaFormatSniffer formatSniffer;

    @Override
    public boolean canExecute(TaskContext context) {
        return true;
    }

    @Override
    public void execute(TaskContext context) {
        // 此节点由 BatchJobService 在 Job 级调用，不在单文件 Plan 中
    }

    /** 扫描源目录，返回嗅探后的文件列表 */
    public List<VfsFile> scan(String providerType, com.unifiedmedia.cms.entity.StorageNode node, String path) {
        StorageAdapter adapter = adapterRegistry.getAdapter(providerType);
        List<VfsFile> files = adapter.listFiles(node, path);

        for (VfsFile f : files) {
            if (!f.isDirectory() && (f.getMimeType() == null || f.getMimeType().isBlank())) {
                String mime = formatSniffer.probeMimeType(f, adapter);
                f.setMimeType(mime);
                f.setFormat(MediaFormat.fromMimeType(mime));
            }
        }
        log.info("[JobSourceScannerNode] Scanned {} files in {}", files.size(), path);
        return files;
    }
}
