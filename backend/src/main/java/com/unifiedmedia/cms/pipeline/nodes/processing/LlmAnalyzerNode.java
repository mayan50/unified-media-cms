package com.unifiedmedia.cms.pipeline.nodes.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifiedmedia.cms.pipeline.core.*;
import com.unifiedmedia.cms.pipeline.payload.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

@Slf4j
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
        String text = context.getPipelineData(PipelineKeys.EXTRACTED_TEXT, String.class);
        return text != null && !text.isBlank();
    }

    @Override
    public void execute(TaskContext context) throws Exception {
        String text = context.getPipelineData(PipelineKeys.EXTRACTED_TEXT, String.class);
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
            context.getAsset().updateTitle((String) result.get("title"));
        if (result.containsKey("author") && result.get("author") instanceof String s && !s.isBlank()) {
            Object authorObj = result.get("author");
            List<String> authors = authorObj instanceof List<?> l ? l.stream().map(Object::toString).toList()
                    : List.of(s);
            setIntentIfUnlocked(context, "authors", PipelineKeys.AUTHORS, authors);
        }
        if (result.containsKey("tags") && result.get("tags") instanceof List<?> tagList) {
            List<String> tags = tagList.stream().map(Object::toString).toList();
            setIntentIfUnlocked(context, "tags", PipelineKeys.TAGS, tags);
        }
        if (result.containsKey("summary") && result.get("summary") != null)
            context.getAsset().updateSummary((String) result.get("summary"));
        String aiTitle = context.getAsset() != null ? context.getAsset().getTitle() : null;
        List<String> aiAuthorList = context.getPipelineList(PipelineKeys.AUTHORS, String.class);
        String aiAuthor = (aiAuthorList != null && !aiAuthorList.isEmpty()) ? aiAuthorList.get(0) : null;
        List<String> aiTags = context.getPipelineList(PipelineKeys.TAGS, String.class);
        context.addLog(aiTitle != null ? "ok" : "warn",
                "LLM分析完成: 标题=" + (aiTitle != null ? aiTitle : "未识别")
                + ", 作者=" + (aiAuthor != null ? aiAuthor : "未识别")
                + ", 标签=" + (aiTags != null ? String.join(",", aiTags) : "无"));
        log.info("[LlmAnalyzerNode] Extracted: title={}, author={}, tags={}",
                aiTitle, aiAuthor, aiTags);
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
