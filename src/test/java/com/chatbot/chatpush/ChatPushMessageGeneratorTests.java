package com.chatbot.chatpush;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chatbot.chatpush.dto.ChatPushCandidate;
import com.chatbot.chatpush.dto.ChatPushContext;
import com.chatbot.config.BotProperties;
import com.chatbot.deepseek.DeepSeekChatService;
import com.chatbot.prompt.PromptContextBuilder;
import com.chatbot.prompt.PromptProfileProvider;
import com.chatbot.prompt.TimeContextProvider;
import com.chatbot.prompt.dto.PromptProfile;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatPushMessageGeneratorTests {

    @Test
    void shouldFallbackWhenAiGenerationFails() {
        DeepSeekChatService deepSeekChatService = org.mockito.Mockito.mock(DeepSeekChatService.class);
        when(deepSeekChatService.chat(anyList())).thenThrow(new IllegalStateException("deepseek down"));
        BotProperties botProperties = new BotProperties();
        botProperties.getChatpush().setUseAiGenerator(true);
        ChatPushMessageGenerator generator = new ChatPushMessageGenerator(
                deepSeekChatService,
                botProperties,
                createPromptContextBuilder(botProperties)
        );

        String message = generator.generate(new ChatPushContext(
                List.of("QQ AI 助手项目"),
                List.of("user：最近在做提醒系统"),
                List.of("继续做 QQ AI 项目"),
                new ChatPushCandidate("REMINDER", "你之前设了继续写 QQ AI 项目的提醒，要不要先把下一步拆一下？"),
                "fallback"
        ));

        assertThat(message).isEqualTo("你之前设了继续写 QQ AI 项目的提醒，要不要先把下一步拆一下？");
    }

    @Test
    void shouldIncludeTimeContextInAiPrompt() {
        DeepSeekChatService deepSeekChatService = org.mockito.Mockito.mock(DeepSeekChatService.class);
        when(deepSeekChatService.chat(contains("[当前时间]"), anyList())).thenReturn("主动消息");
        BotProperties botProperties = new BotProperties();
        botProperties.setTimezone("Asia/Shanghai");
        botProperties.getChatpush().setUseAiGenerator(true);
        ChatPushMessageGenerator generator = new ChatPushMessageGenerator(
                deepSeekChatService,
                botProperties,
                createPromptContextBuilder(botProperties)
        );

        String message = generator.generate(new ChatPushContext(
                List.of("长期记忆"),
                List.of("最近聊天"),
                List.of("待办"),
                new ChatPushCandidate("REMINDER", "建议方向"),
                "fallback"
        ));

        assertThat(message).isEqualTo("主动消息");
        verify(deepSeekChatService).chat(contains("[当前时间]"), anyList());
    }

    private PromptContextBuilder createPromptContextBuilder(BotProperties properties) {
        PromptProfileProvider provider = new PromptProfileProvider() {
            @Override
            public PromptProfile getProfile() {
                return new PromptProfile(PromptProfile.Source.DEFAULT, "persona", 7, false);
            }

            @Override
            public PromptProfile reload() {
                return getProfile();
            }
        };
        TimeContextProvider timeContextProvider = TimeContextProvider.forClock(
                properties,
                Clock.fixed(Instant.parse("2026-05-04T16:03:21Z"), ZoneId.of("UTC"))
        );
        return new PromptContextBuilder(provider, timeContextProvider, properties);
    }
}
