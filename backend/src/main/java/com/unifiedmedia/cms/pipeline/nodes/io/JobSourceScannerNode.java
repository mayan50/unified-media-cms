package com.unifiedmedia.cms.pipeline.nodes.io;

import com.unifiedmedia.cms.entity.StorageNode;
import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.core.annotation.NodeDef;
import com.unifiedmedia.cms.pipeline.payload.PipelineKeys;
import com.unifiedmedia.cms.repository.StorageNodeRepository;
import com.unifiedmedia.cms.pipeline.storage.StorageAdapterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Job 级源目录扫描器 — 系统隐式节点。
 * <p>
 * 严格遵循 {@link PipelineNode} 契约，通过 {@link #execute(TaskContext)} 执行扫描，
 * 产出 {@code List<VfsFile>} 写入 {@code FILE_CANDIDATES}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@NodeDef(name = "JobSourceScannerNode", label = "源目录扫描", icon = "🔍",
        type = NodeType.SYSTEM, hiddenFromUI = true)
public class JobSourceScannerNode implements PipelineNode {

    private final StorageAdapterRegistry adapterRegistry;
    private final MediaFormatSniffer formatSniffer;
    private final StorageNodeRepository storageNodeRepository;

    @Override
    public boolean canExecute(TaskContext context) {
        return context.getPipelineData(PipelineKeys.SOURCE_DIRECTORY, String.class) != null
                || context.getPipelineData(PipelineKeys.STORAGE_NODE_ID, UUID.class) != null;
    }

    @Override
    public void execute(TaskContext context) {
        UUID storageNodeId = context.getPipelineData(PipelineKeys.STORAGE_NODE_ID, UUID.class);
        String path = context.getPipelineData(PipelineKeys.SOURCE_DIRECTORY, String.class);

        // 单文件模式：SOURCE_DIRECTORY 就是文件路径本身
        if (storageNodeId == null && path != null && path.matches(".*\\.(txt|TXT|epub|EPUB|pdf|PDF|mp4|mkv)$")) {
            VfsFile f = VfsFile.builder().remotePath(path)
                    .fileName(java.nio.file.Path.of(path).getFileName().toString())
                    .mimeType(null)
                    .format(MediaFormat.fromExtension(path)).build();
            context.setPipelineData(PipelineKeys.FILE_CANDIDATES, List.of(f));
            log.info("[JobSourceScannerNode] Single file mode: {}", f.getFileName());
            return;
        }

        StorageNode node = storageNodeId != null ? storageNodeRepository.findById(storageNodeId).orElse(null) : null;
        if (node == null || path == null) {
            context.setPipelineData(PipelineKeys.FILE_CANDIDATES, List.of());
            return;
        }

        StorageAdapter adapter = adapterRegistry.getAdapter(node.getProviderType());
        List<VfsFile> files = adapter.listFiles(node, path);
        List<VfsFile> result = new ArrayList<>();
        for (VfsFile f : files) {
            if (!f.isDirectory() && (f.getMimeType() == null || f.getMimeType().isBlank())) {
                String mime = formatSniffer.probeMimeType(f, adapter);
                f.setMimeType(mime);
                f.setFormat(MediaFormat.fromMimeType(mime));
            }
            result.add(f);
        }
        context.setPipelineData(PipelineKeys.FILE_CANDIDATES, result);
        log.info("[JobSourceScannerNode] Scanned {} files in {}", result.size(), path);
    }
}
