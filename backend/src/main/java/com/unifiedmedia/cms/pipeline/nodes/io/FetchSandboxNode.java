package com.unifiedmedia.cms.pipeline.nodes.io;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.core.annotation.NodeDef;
import com.unifiedmedia.cms.pipeline.payload.PipelineKeys;
import com.unifiedmedia.cms.pipeline.storage.StorageAdapterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * 沙箱拉取器 — Task 级系统隐式头节点。
 * <p>
 * 在单文件流水线首个位置被 Orchestrator 强制插入，
 * 调用 {@link StorageAdapter#downloadToSandbox} 将文件拉取到本地沙箱。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@NodeDef(name = "FetchSandboxNode", label = "拉取沙箱", icon = "📥",
        type = NodeType.SYSTEM, hiddenFromUI = true)
public class FetchSandboxNode implements PipelineNode {

    private final StorageAdapterRegistry adapterRegistry;

    @Override
    public boolean canExecute(TaskContext context) {
        return context.getPipelineData(PipelineKeys.TEMP_SANDBOX_PATH, String.class) == null;
    }

    @Override
    public void execute(TaskContext context) {
        VfsFile target = context instanceof PipelineTaskContext ptc ? ptc.getTargetFile() : null;
        if (target == null || target.getProviderType() == null) {
            context.addLog("warn", "FetchSandbox: 无目标文件，跳过拉取");
            return;
        }
        StorageAdapter adapter = adapterRegistry.getAdapter(target.getProviderType());
        Path localPath = adapter.downloadToSandbox(null, target,
                context.getTaskId() != null ? context.getTaskId().toString() : "unknown");
        context.setPipelineData(PipelineKeys.TEMP_SANDBOX_PATH, localPath.toString());
        context.addLog("ok", "文件拉取到沙箱: " + localPath);
        log.debug("[FetchSandboxNode] {} → sandbox {}", target.getFileName(), localPath);
    }
}
