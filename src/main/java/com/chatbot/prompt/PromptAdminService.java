package com.chatbot.prompt;

import com.chatbot.config.BotProperties;
import com.chatbot.prompt.dto.PromptProfile;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromptAdminService {

    private final BotProperties botProperties;
    private final PromptProfileProvider promptProfileProvider;

    public PromptAdminService(BotProperties botProperties, PromptProfileProvider promptProfileProvider) {
        this.botProperties = botProperties;
        this.promptProfileProvider = promptProfileProvider;
    }

    public String handleCommand(String text) {
        if ("/prompt status".equals(text)) {
            return renderStatus();
        }
        if ("/prompt reload".equals(text)) {
            return reload();
        }
        return null;
    }

    private String renderStatus() {
        PromptProfile profile = promptProfileProvider.getProfile();
        return "Prompt 状态："
                + "\nprofile 来源：" + profile.source().name().toLowerCase(Locale.ROOT)
                + "\nprofile 长度：" + profile.length() + " 字符"
                + "\n启用时间上下文：" + botProperties.getPrompt().isIncludeTimeContext()
                + "\n当前时区：" + botProperties.promptZoneId().getId()
                + "\nreload-file-each-request：" + botProperties.getPrompt().isReloadFileEachRequest()
                + "\nprofile-file 已配置：" + StringUtils.hasText(botProperties.getPrompt().getProfileFile())
                + "\nprofile 已截断：" + profile.truncated();
    }

    private String reload() {
        if (botProperties.getPrompt().isReloadFileEachRequest()) {
            return "当前已开启每次请求自动重载，无需手动 reload。";
        }
        PromptProfile before = promptProfileProvider.getProfile();
        PromptProfile after = promptProfileProvider.reload();
        if (after.source() == PromptProfile.Source.FILE && StringUtils.hasText(botProperties.getPrompt().getProfileFile())) {
            return "已重新加载提示词文件。";
        }
        if (before.source() == after.source() && before.length() == after.length()) {
            return "提示词文件加载失败，继续使用上一次可用配置。";
        }
        return "提示词文件加载失败，已使用兜底提示词。";
    }
}
