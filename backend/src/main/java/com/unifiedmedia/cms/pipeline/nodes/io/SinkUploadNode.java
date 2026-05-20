package com.unifiedmedia.cms.pipeline.nodes.io;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.core.annotation.NodeDef;
import com.unifiedmedia.cms.pipeline.payload.PipelineKeys;
import com.unifiedmedia.cms.service.StorageAdapterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * 汇点上传器 — Task 级系统隐式尾节点。
 * <p>
 * 在单文件流水线最后一个位置被 Orchestrator 强制插入，
 * 调用 {@link StorageAdapter#uploadFromSandbox} 将处理完成的沙箱文件推回远端存储。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@NodeDef(name = "SinkUploadNode", label = "上传远端", icon = "📤",
        type = NodeType.SYSTEM, hiddenFromUI = true)
public class SinkUploadNode implements PipelineNode {

    private final StorageAdapterRegistry adapterRegistry;

    @Override
    public boolean canExecute(TaskContext context) {
        return context.getPipelineData(PipelineKeys.TEMP_SANDBOX_PATH, String.class) != null;
    }

    @Override
    public void execute(TaskContext context) {
        String sandboxPath = context.getPipelineData(PipelineKeys.TEMP_SANDBOX_PATH, String.class);
        if (sandboxPath == null) {
            context.addLog("warn", "SinkUpload: 无沙箱文件路径，跳过上传");
            return;
        }
        VfsFile target = context instanceof PipelineTaskContext ptc ? ptc.getTargetFile() : null;
        if (target == null || target.getProviderType() == null) {
            context.addLog("warn", "SinkUpload: 无目标文件，跳过上传");
            return;
        }
        StorageAdapter adapter = adapterRegistry.getAdapter(target.getProviderType());
        adapter.uploadFromSandbox(null, Path.of(sandboxPath), target.getRemotePath());
        context.addLog("ok", "文件已推回远端: " + target.getFileName());
        log.debug("[SinkUploadNode] sandbox {} → {}", sandboxPath, target.getFileName());
    }
}
