package com.chatbot.chatpush;

import com.chatbot.chat.ChatMessageContext;
import com.chatbot.chatpush.dto.ChatPushContext;
import com.chatbot.config.BotProperties;
import com.chatbot.deepseek.DeepSeekChatService;
import com.chatbot.prompt.PromptContextBuilder;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ChatPushMessageGenerator {

    private static final String CHAT_PUSH_TASK_PROMPT = """
            你要生成一条主动发起的话题消息。
            需要结合长期记忆、最近聊天、待办提醒和建议方向判断当前话题是否合时宜。
            输出限制：
            - 只输出一条中文消息。
            - 自然、简短、不打扰。
            - 不超过 60 字。
            - 不要连续追问多个问题。
            - 不要假装知道不存在的事实。
            """;

    private final DeepSeekChatService deepSeekChatService;
    private final BotProperties botProperties;
    private final PromptContextBuilder promptContextBuilder;

    public ChatPushMessageGenerator(DeepSeekChatService deepSeekChatService,
                                    BotProperties botProperties,
                                    PromptContextBuilder promptContextBuilder) {
        this.deepSeekChatService = deepSeekChatService;
        this.botProperties = botProperties;
        this.promptContextBuilder = promptContextBuilder;
    }

    public String generate(ChatPushContext context) {
        String fallback = context.candidate() == null ? context.fallbackMessage() : context.candidate().summary();
        if (!botProperties.getChatpush().isUseAiGenerator()) {
            return fallback;
        }
        try {
            String prompt = buildPrompt(context);
            String systemPrompt = promptContextBuilder.buildSystemPrompt(CHAT_PUSH_TASK_PROMPT);
            String reply = deepSeekChatService.chat(systemPrompt, List.of(new ChatMessageContext("user", prompt)));
            return StringUtils.hasText(reply) ? reply.trim() : fallback;
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String buildPrompt(ChatPushContext context) {
        return "[长期记忆]\n"
                + joinLines(context.memories())
                + "\n\n[最近聊天]\n"
                + joinLines(context.recentMessages())
                + "\n\n[待办提醒]\n"
                + joinLines(context.pendingTasks())
                + "\n\n[建议方向]\n"
                + (context.candidate() == null ? context.fallbackMessage() : context.candidate().summary())
                + "\n\n请生成一条现在适合发送的主动消息。";
    }

    private String joinLines(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "- 无";
        }
        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            builder.append("- ").append(item).append('\n');
        }
        return builder.toString().trim();
    }
}
