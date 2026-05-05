package com.chatbot.proactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
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

class ReminderPromptIntegrationTests {

    @Test
    void aiIntentPromptShouldIncludeTimeContext() {
        DeepSeekChatService deepSeekChatService = org.mockito.Mockito.mock(DeepSeekChatService.class);
        when(deepSeekChatService.chat(contains("[当前时间]"), anyList()))
                .thenReturn("{\"type\":\"LIST_REMINDER\",\"timeText\":\"\",\"content\":\"\",\"confidence\":0.9}");
        DeepSeekReminderAiIntentService service = new DeepSeekReminderAiIntentService(
                deepSeekChatService,
                createPromptContextBuilder(),
                new ObjectMapper()
        );

        ReminderIntent intent = service.detect("明天提醒我");

        assertThat(intent.type()).isEqualTo(ReminderIntentType.LIST_REMINDER);
        verify(deepSeekChatService).chat(contains("[当前时间]"), anyList());
    }

    @Test
    void aiReplyPromptShouldIncludeTimeContext() {
        DeepSeekChatService deepSeekChatService = org.mockito.Mockito.mock(DeepSeekChatService.class);
        when(deepSeekChatService.chat(contains("[当前时间]"), anyList())).thenReturn("好的");
        DeepSeekReminderReplyGenerator generator = new DeepSeekReminderReplyGenerator(
                deepSeekChatService,
                createPromptContextBuilder()
        );

        String reply = generator.generate(ReminderReplyAction.CREATED, null, "起床", List.of());

        assertThat(reply).isEqualTo("好的");
        verify(deepSeekChatService).chat(contains("[当前时间]"), anyList());
    }

    private PromptContextBuilder createPromptContextBuilder() {
        BotProperties properties = new BotProperties();
        properties.setTimezone("Asia/Shanghai");
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
