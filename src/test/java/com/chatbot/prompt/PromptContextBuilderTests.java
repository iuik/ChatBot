package com.chatbot.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.chatbot.config.BotProperties;
import com.chatbot.prompt.dto.PromptProfile;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class PromptContextBuilderTests {

    @Test
    void shouldIncludeTimeContextByDefault() {
        BotProperties properties = new BotProperties();
        properties.setTimezone("Asia/Shanghai");
        PromptProfileProvider provider = fixedProvider();
        TimeContextProvider timeContextProvider = TimeContextProvider.forClock(
                properties,
                Clock.fixed(Instant.parse("2026-05-04T16:03:21Z"), ZoneId.of("UTC"))
        );
        PromptContextBuilder builder = new PromptContextBuilder(provider, timeContextProvider, properties);

        String prompt = builder.buildSystemPrompt("test task");

        assertThat(prompt).contains("[人物设定]");
        assertThat(prompt).contains("persona");
        assertThat(prompt).contains("[当前时间]");
        assertThat(prompt).contains("当前时间：2026-05-05 00:03:21");
        assertThat(prompt).contains("[日常聊天风格]");
        assertThat(prompt).contains("命令、系统、模块、配置");
    }

    @Test
    void shouldSkipTimeContextWhenDisabled() {
        BotProperties properties = new BotProperties();
        properties.getPrompt().setIncludeTimeContext(false);
        PromptProfileProvider provider = fixedProvider();
        TimeContextProvider timeContextProvider = TimeContextProvider.forClock(
                properties,
                Clock.fixed(Instant.parse("2026-05-04T16:03:21Z"), ZoneId.of("UTC"))
        );
        PromptContextBuilder builder = new PromptContextBuilder(provider, timeContextProvider, properties);

        String prompt = builder.buildSystemPrompt();

        assertThat(prompt).doesNotContain("[当前时间]");
        assertThat(prompt).contains("[日常聊天风格]");
    }

    private PromptProfileProvider fixedProvider() {
        return new PromptProfileProvider() {
            @Override
            public PromptProfile getProfile() {
                return new PromptProfile(PromptProfile.Source.ENV, "persona", 7, false);
            }

            @Override
            public PromptProfile reload() {
                return getProfile();
            }
        };
    }
}
