package com.unifiedmedia.cms.pipeline.nodes.control;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class WaitNode extends BaseFlowControlNode {

    public WaitNode() {
        super("WaitNode", "延时等待", "⏱",
                "暂停指定毫秒数后继续执行",
                List.of(ConfigFieldDef.number("delayMs", "等待(ms)", "1000")),
                List.of("delayMs"));
    }

    @Override
    public boolean canExecute(TaskContext context) {
        return true;
    }

    @Override
    public void execute(TaskContext context) throws InterruptedException {
        Object delayObj = context.getPipelineData(PipelineKeys.DELAY_MS, Object.class);
        long delayMs = 1000;
        if (delayObj instanceof Number n) delayMs = n.longValue();
        else if (delayObj instanceof String s) {
            try { delayMs = Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        log.info("[WaitNode] Waiting {}ms", delayMs);
        Thread.sleep(delayMs);
        context.addLog("ok", "等待 " + delayMs + "ms");
    }
}
