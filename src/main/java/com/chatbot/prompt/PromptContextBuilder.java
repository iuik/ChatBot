package com.chatbot.prompt;

import com.chatbot.config.BotProperties;
import com.chatbot.prompt.dto.PromptProfile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PromptContextBuilder {

    private static final String DEFAULT_RULES = """
            [回答规则]
            - 默认使用中文。
            - 回答要自然、直接、简洁。
            - 如果不确定，不要编造，明确说明不确定。
            - 涉及技术问题时优先给出可执行步骤和风险提示。
            """;

    private static final String DAILY_CHAT_RULES = """
            [日常聊天风格]
            - 日常聊天要像真实 QQ 私聊，多用短句。
            - 不要总是一次性解释完，保持自然节奏。
            - 不要频繁使用“你是想A还是B”“我可以帮你”“需要我帮你吗”这类套话。
            - 除非用户正在明确讨论技术或项目，不要主动提到命令、系统、模块、配置等后台词。
            """;

    private final PromptProfileProvider promptProfileProvider;
    private final TimeContextProvider timeContextProvider;
    private final BotProperties botProperties;

    public PromptContextBuilder(PromptProfileProvider promptProfileProvider,
                                TimeContextProvider timeContextProvider,
                                BotProperties botProperties) {
        this.promptProfileProvider = promptProfileProvider;
        this.timeContextProvider = timeContextProvider;
        this.botProperties = botProperties;
    }

    public String buildSystemPrompt() {
        return buildSystemPrompt(null);
    }

    public String buildSystemPrompt(String taskInstructions) {
        PromptProfile profile = promptProfileProvider.getProfile();
        StringBuilder builder = new StringBuilder();
        builder.append("[人物设定]\n").append(profile.text().trim());
        if (botProperties.getPrompt().isIncludeTimeContext()) {
            builder.append("\n\n").append(timeContextProvider.getCurrentTimeContext().formattedBlock());
        }
        builder.append("\n\n").append(DEFAULT_RULES.trim());
        builder.append("\n\n").append(DAILY_CHAT_RULES.trim());
        if (StringUtils.hasText(taskInstructions)) {
            builder.append("\n\n[任务要求]\n").append(taskInstructions.trim());
        }
        return builder.toString().trim();
    }
}
