package com.chatbot.proactive;

import static org.assertj.core.api.Assertions.assertThat;

import com.chatbot.config.BotProperties;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ReminderReplyServiceTests {

    @Test
    void shouldFallbackToTemplateWhenAiReplyFails() {
        BotProperties botProperties = new BotProperties();
        botProperties.getReminder().setAiReplyEnabled(true);
        botProperties.getProactive().setTimezone("Asia/Shanghai");
        ReminderReplyService service = new ReminderReplyService(
                botProperties,
                (action, scheduledAt, content, candidates) -> {
                    throw new IllegalStateException("ai down");
                }
        );

        String reply = service.created(LocalDateTime.of(2026, 5, 5, 8, 0), "叫我起床");

        assertThat(reply).isEqualTo("好，今天早上 8 点我叫你起床。");
    }
}
