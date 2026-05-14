package com.unifiedmedia.cms.poc;

import com.unifiedmedia.cms.pipeline.core.BaseProcessingNode;
import com.unifiedmedia.cms.pipeline.core.TaskContext;

import java.util.List;

public class DummyNode extends BaseProcessingNode {

    private final String version;

    public DummyNode() { this("2"); }

    public DummyNode(String version) {
        super("DummyNode", "哑炮测试 v" + version, "🧪",
                "PF4J 热加载 POC — 版本 " + version,
                List.of(),
                List.of());
        this.version = version;
    }

    @Override
    public boolean canExecute(TaskContext context) {
        return true;
    }

    @Override
    public void execute(TaskContext context) {
        context.addLog("ok", "====== Hello from Plugin V" + version + " ======");
        System.out.println("====== Hello from Plugin V" + version + " ======");
    }
}
