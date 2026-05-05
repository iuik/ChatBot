package com.chatbot.prompt;

import com.chatbot.config.BotProperties;
import com.chatbot.prompt.dto.PromptProfile;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DefaultPromptProfileProvider implements PromptProfileProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultPromptProfileProvider.class);

    static final String DEFAULT_PROFILE_TEXT = """
            你是用户的 QQ 私聊 AI 助手。
            默认使用中文回答。
            语气自然、直接、简洁。
            用户更需要现实判断、可执行方案和有依据的回答，不需要空泛鼓励。
            涉及技术问题时，优先给出步骤、命令、风险点和排错方法。
            如果不确定，不要编造，直接说明不确定。
            """;

    private final BotProperties botProperties;
    private final FilePromptProfileProvider filePromptProfileProvider;

    private volatile PromptProfile cachedProfile;

    public DefaultPromptProfileProvider(BotProperties botProperties,
                                        FilePromptProfileProvider filePromptProfileProvider) {
        this.botProperties = botProperties;
        this.filePromptProfileProvider = filePromptProfileProvider;
    }

    @PostConstruct
    void initialize() {
        cachedProfile = resolveProfile(null);
    }

    @Override
    public PromptProfile getProfile() {
        if (botProperties.getPrompt().isReloadFileEachRequest()) {
            PromptProfile profile = resolveProfile(cachedProfile);
            cachedProfile = profile;
            return profile;
        }
        if (cachedProfile == null) {
            cachedProfile = resolveProfile(null);
        }
        return cachedProfile;
    }

    @Override
    public PromptProfile reload() {
        PromptProfile profile = resolveProfile(cachedProfile);
        cachedProfile = profile;
        return profile;
    }

    private PromptProfile resolveProfile(PromptProfile previousProfile) {
        List<String> sources = sourcePriority();
        for (String source : sources) {
            if ("env".equals(source)) {
                PromptProfile profile = fromEnv();
                if (profile != null) {
                    return profile;
                }
                continue;
            }
            PromptProfile profile = fromFile();
            if (profile != null) {
                return profile;
            }
        }
        if (previousProfile != null) {
            return previousProfile;
        }
        return normalize(PromptProfile.Source.DEFAULT, DEFAULT_PROFILE_TEXT);
    }

    private List<String> sourcePriority() {
        String priority = botProperties.getPrompt().getProfileSourcePriority();
        if ("env-first".equalsIgnoreCase(priority)) {
            return List.of("env", "file");
        }
        return List.of("file", "env");
    }

    private PromptProfile fromEnv() {
        if (!StringUtils.hasText(botProperties.getPrompt().getProfileText())) {
            return null;
        }
        return normalize(PromptProfile.Source.ENV, botProperties.getPrompt().getProfileText());
    }

    private PromptProfile fromFile() {
        String profileFile = botProperties.getPrompt().getProfileFile();
        if (!StringUtils.hasText(profileFile)) {
            return null;
        }
        try {
            String text = filePromptProfileProvider.read(profileFile.trim());
            if (!StringUtils.hasText(text)) {
                log.warn("Prompt profile file is empty, path={}", profileFile);
                return null;
            }
            return normalize(PromptProfile.Source.FILE, text);
        } catch (Exception ex) {
            log.warn("Failed to read prompt profile file, path={}", profileFile);
            return null;
        }
    }

    private PromptProfile normalize(PromptProfile.Source source, String text) {
        String normalized = text == null ? "" : text.trim();
        int maxChars = Math.max(botProperties.getPrompt().getMaxProfileChars(), 1);
        boolean truncated = normalized.length() > maxChars;
        if (truncated) {
            log.warn("Prompt profile truncated, source={}, originalLength={}, maxChars={}",
                    source.name().toLowerCase(Locale.ROOT), normalized.length(), maxChars);
            normalized = normalized.substring(0, maxChars);
        }
        log.info("Prompt profile ready, source={}, length={}",
                source.name().toLowerCase(Locale.ROOT), normalized.length());
        return new PromptProfile(source, normalized, normalized.length(), truncated);
    }
}
