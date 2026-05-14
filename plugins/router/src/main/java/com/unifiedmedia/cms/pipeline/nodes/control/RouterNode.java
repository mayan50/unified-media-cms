package com.unifiedmedia.cms.pipeline.nodes.control;

import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class RouterNode extends BaseFlowControlNode {

    public RouterNode() {
        super("RouterNode", "条件分支", "🔀",
                "根据条件表达式将数据流分发到不同分支",
                List.of(ConfigFieldDef.text("condition", "条件表达式", "detectedFormat == 'TXT'")),
                List.of("condition"));
    }

    @Override
    public boolean canExecute(TaskContext context) {
        return context.getPipelineData(PipelineKeys.DETECTED_FORMAT, String.class) != null;
    }

    @Override
    public void execute(TaskContext context) {
        String condition = context.getPipelineData(PipelineKeys.ROUTER_CONDITION, String.class);
        if (condition == null || condition.isBlank()) {
            String format = context.getPipelineData(PipelineKeys.DETECTED_FORMAT, String.class);
            context.setPipelineData(PipelineKeys.ROUTER_BRANCH, "TXT".equals(format) ? "true" : "false");
        } else {
            boolean result = evaluateCondition(condition, context);
            context.setPipelineData(PipelineKeys.ROUTER_BRANCH, String.valueOf(result));
        }
        String branch = String.valueOf(context.getPipelineData(PipelineKeys.ROUTER_BRANCH, String.class));
        context.addLog("ok", "条件分支: " + branch);
        log.info("[RouterNode] Branch resolved: routerBranch={}", branch);
    }

    private boolean evaluateCondition(String condition, TaskContext context) {
        String expr = condition.trim();
        if (expr.contains("==")) {
            String[] parts = expr.split("==");
            if (parts.length == 2) {
                String left = resolveValue(parts[0].trim(), context);
                String right = parts[1].trim().replace("'", "").replace("\"", "");
                return left.equalsIgnoreCase(right);
            }
        }
        if (expr.contains("!=")) {
            String[] parts = expr.split("!=");
            if (parts.length == 2) {
                String left = resolveValue(parts[0].trim(), context);
                String right = parts[1].trim().replace("'", "").replace("\"", "");
                return !left.equalsIgnoreCase(right);
            }
        }
        return false;
    }

    private String resolveValue(String token, TaskContext context) {
        if ("detectedFormat".equals(token)) {
            String v = context.getPipelineData(PipelineKeys.DETECTED_FORMAT, String.class);
            return v != null ? v : "";
        }
        if ("detectedMimeType".equals(token)) {
            String v = context.getPipelineData(PipelineKeys.DETECTED_MIME_TYPE, String.class);
            return v != null ? v : "";
        }
        return token;
    }
}
