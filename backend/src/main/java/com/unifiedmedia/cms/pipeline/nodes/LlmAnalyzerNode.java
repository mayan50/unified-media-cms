package com.unifiedmedia.cms.pipeline.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifiedmedia.cms.pipeline.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class LlmAnalyzerNode extends BaseProcessingNode {

    private final ObjectMapper objectMapper;
    private final OllamaChatModel ollamaChatModel;
    private final OpenAiChatModel openAiChatModel;

    @Value("${app.llm.provider:ollama}")
    private String llmProvider;

    private static final String SYSTEM_PROMPT = """
            你是一个图书元数据提取专家。用户将提供一段书籍文本内容，你需要提取以下信息并以 JSON 格式返回：
            {"title": "书名", "author": "作者", "tags": ["标签1", "标签2"], "summary": "简介"}
            如果无法确定某个字段，请填入 null。只返回 JSON，不要其他内容。
            """;

    public LlmAnalyzerNode(ObjectMapper objectMapper,
                           OllamaChatModel ollamaChatModel,
                           OpenAiChatModel openAiChatModel) {
        super("LlmAnalyzerNode", "大模型分析", "🤖",
                "调用大语言模型（Ollama / OpenAI）从文本中提取结构化元数据",
                List.of(ConfigFieldDef.textarea("prompt", "Prompt", "")),
                List.of());
        this.objectMapper = objectMapper;
        this.ollamaChatModel = ollamaChatModel;
        this.openAiChatModel = openAiChatModel;
    }

    @Override
    public boolean canExecute(TaskContext context) {
        return context.getExtractedText() != null && !context.getExtractedText().isBlank();
    }

    @Override
    public void execute(TaskContext context) throws Exception {
        String text = context.getExtractedText();
        ChatClient chatClient = "openai".equalsIgnoreCase(llmProvider)
                ? ChatClient.create(openAiChatModel)
                : ChatClient.create(ollamaChatModel);
        String response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("请分析以下文本：\n" + text)
                .call().content();
        log.info("[LlmAnalyzerNode] Raw LLM response: {}", response);
        Map<String, Object> result = parseLlmResponse(response);
        if (result.containsKey("title") && result.get("title") != null)
            context.setAiTitle((String) result.get("title"));
        if (result.containsKey("author") && result.get("author") != null)
            context.setAiAuthor((String) result.get("author"));
        if (result.containsKey("tags") && result.get("tags") != null) {
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) result.get("tags");
            context.setAiTags(tags);
        }
        if (result.containsKey("summary") && result.get("summary") != null)
            context.setAiSummary((String) result.get("summary"));
        log.info("[LlmAnalyzerNode] Extracted: title={}, author={}, tags={}",
                context.getAiTitle(), context.getAiAuthor(), context.getAiTags());
    }

    private Map<String, Object> parseLlmResponse(String response) {
        try {
            String json = response.trim();
            if (json.contains("```json")) {
                json = json.substring(json.indexOf("```json") + 7);
                json = json.substring(0, json.indexOf("```"));
            } else if (json.contains("```")) {
                json = json.substring(json.indexOf("```") + 3);
                json = json.substring(0, json.indexOf("```"));
            }
            json = json.trim();
            JsonNode node = objectMapper.readTree(json);
            Map<String, Object> result = new HashMap<>();
            if (node.has("title")) result.put("title", node.get("title").asText());
            if (node.has("author")) result.put("author", node.get("author").asText());
            if (node.has("summary")) result.put("summary", node.get("summary").asText());
            if (node.has("tags")) {
                List<String> tags = new ArrayList<>();
                node.get("tags").forEach(t -> tags.add(t.asText()));
                result.put("tags", tags);
            }
            return result;
        } catch (Exception e) {
            log.warn("[LlmAnalyzerNode] Failed to parse LLM response: {}", e.getMessage());
            return Map.of();
        }
    }
}
