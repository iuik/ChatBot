package com.chatbot.proactive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chatbot.chat.ChatMessageContext;
import com.chatbot.deepseek.DeepSeekChatService;
import com.chatbot.prompt.PromptContextBuilder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DeepSeekReminderAiIntentService implements ReminderAiIntentService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekReminderAiIntentService.class);

    private static final String TASK_PROMPT = """
            你要辅助识别提醒意图。
            只允许识别三种类型：CREATE_REMINDER、DELETE_REMINDER、LIST_REMINDER。
            如果无法确认，返回 UNKNOWN。
            输出必须是 JSON，对象字段如下：
            - type: CREATE_REMINDER / DELETE_REMINDER / LIST_REMINDER / UNKNOWN
            - timeText: 字符串，没有则用空字符串
            - content: 字符串，没有则用空字符串
            - confidence: 0 到 1 的数字
            不要输出 JSON 之外的任何内容。
            """;

    private final DeepSeekChatService deepSeekChatService;
    private final PromptContextBuilder promptContextBuilder;
    private final ObjectMapper objectMapper;

    public DeepSeekReminderAiIntentService(DeepSeekChatService deepSeekChatService,
                                           PromptContextBuilder promptContextBuilder,
                                           ObjectMapper objectMapper) {
        this.deepSeekChatService = deepSeekChatService;
        this.promptContextBuilder = promptContextBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReminderIntent detect(String normalizedText) {
        String systemPrompt = promptContextBuilder.buildSystemPrompt(TASK_PROMPT);
        String userPrompt = "[用户消息]\n" + normalizedText;
        try {
            String reply = deepSeekChatService.chat(systemPrompt, List.of(new ChatMessageContext("user", userPrompt)));
            JsonNode root = objectMapper.readTree(reply);
            ReminderIntentType type = ReminderIntentType.valueOf(root.path("type").asText("UNKNOWN"));
            String timeText = emptyToNull(root.path("timeText").asText());
            String content = emptyToNull(root.path("content").asText());
            double confidence = root.path("confidence").asDouble(0.0);
            return new ReminderIntent(type, timeText, content, confidence, normalizedText);
        } catch (Exception ex) {
            log.warn("Failed to detect reminder intent with AI");
            return ReminderIntent.unknown(normalizedText);
        }
    }

    private String emptyToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
