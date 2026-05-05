package com.chatbot.proactive;

import com.chatbot.chat.ChatMessageContext;
import com.chatbot.deepseek.DeepSeekChatService;
import com.chatbot.prompt.PromptContextBuilder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DeepSeekReminderReplyGenerator implements ReminderReplyGenerator {

    private static final String TASK_PROMPT = """
            你要生成提醒相关回复。
            要求：
            - 默认中文。
            - 自然、简洁、像私聊回复。
            - 1 到 2 句话。
            - 不要输出解释性前缀。
            """;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DeepSeekChatService deepSeekChatService;
    private final PromptContextBuilder promptContextBuilder;

    public DeepSeekReminderReplyGenerator(DeepSeekChatService deepSeekChatService,
                                          PromptContextBuilder promptContextBuilder) {
        this.deepSeekChatService = deepSeekChatService;
        this.promptContextBuilder = promptContextBuilder;
    }

    @Override
    public String generate(ReminderReplyAction action,
                           LocalDateTime scheduledAt,
                           String content,
                           List<String> candidates) {
        String systemPrompt = promptContextBuilder.buildSystemPrompt(TASK_PROMPT);
        String userPrompt = buildUserPrompt(action, scheduledAt, content, candidates);
        return deepSeekChatService.chat(systemPrompt, List.of(new ChatMessageContext("user", userPrompt)));
    }

    private String buildUserPrompt(ReminderReplyAction action,
                                   LocalDateTime scheduledAt,
                                   String content,
                                   List<String> candidates) {
        StringBuilder builder = new StringBuilder("[提醒动作]\n").append(action.name());
        if (scheduledAt != null) {
            builder.append("\n\n[提醒时间]\n").append(scheduledAt.format(DATE_TIME_FORMATTER));
        }
        if (content != null && !content.isBlank()) {
            builder.append("\n\n[提醒内容]\n").append(content.trim());
        }
        if (candidates != null && !candidates.isEmpty()) {
            builder.append("\n\n[候选提醒]\n");
            for (String candidate : candidates) {
                builder.append("- ").append(candidate).append('\n');
            }
        }
        builder.append("\n\n请生成适合当前动作的回复。");
        return builder.toString().trim();
    }
}
