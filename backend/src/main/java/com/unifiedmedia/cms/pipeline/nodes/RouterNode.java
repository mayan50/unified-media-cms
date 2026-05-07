package com.unifiedmedia.cms.pipeline.nodes;

import com.unifiedmedia.cms.pipeline.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RouterNode extends BaseFlowControlNode {

    public RouterNode() {
        super("RouterNode", "条件分支", "🔀",
                "根据条件表达式将数据流分发到不同分支",
                List.of(ConfigFieldDef.text("condition", "条件表达式", "detectedFormat == 'TXT'")),
                List.of("condition"));
    }

    @Override
    public boolean canExecute(TaskContext context) {
        return context.getDetectedFormat() != null;
    }

    @Override
    public void execute(TaskContext context) {
        String condition = context.get("routerCondition");
        if (condition == null || condition.isBlank()) {
            String format = context.getDetectedFormat();
            context.put("routerBranch", "TXT".equals(format) ? "true" : "false");
        } else {
            boolean result = evaluateCondition(condition, context);
            context.put("routerBranch", String.valueOf(result));
        }
        log.info("[RouterNode] Branch resolved: routerBranch={}", String.valueOf(context.get("routerBranch")));
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
        return switch (token) {
            case "detectedFormat" -> context.getDetectedFormat() != null ? context.getDetectedFormat() : "";
            case "detectedMimeType" -> context.getDetectedMimeType() != null ? context.getDetectedMimeType() : "";
            default -> token;
        };
    }
}
