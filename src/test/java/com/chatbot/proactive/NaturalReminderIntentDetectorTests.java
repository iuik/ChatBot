package com.chatbot.proactive;

import static org.assertj.core.api.Assertions.assertThat;

import com.chatbot.config.BotProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class NaturalReminderIntentDetectorTests {

    @Test
    void lowConfidenceAiIntentShouldNotCreateReminder() {
        BotProperties botProperties = new BotProperties();
        botProperties.getProactive().setTimezone("Asia/Shanghai");
        botProperties.getReminder().setAiIntentEnabled(true);
        ReminderTimeParser parser = new ReminderTimeParser(
                Clock.fixed(Instant.parse("2026-05-03T01:00:00Z"), ZoneId.of("UTC")),
                new ReminderTextNormalizer()
        );
        NaturalReminderIntentDetector detector = new NaturalReminderIntentDetector(
                botProperties,
                parser,
                text -> new ReminderIntent(ReminderIntentType.CREATE_REMINDER, "明天8点", "起床", 0.5, text)
        );

        ReminderIntent intent = detector.detect("帮我处理一下明天的事情");

        assertThat(intent.type()).isEqualTo(ReminderIntentType.UNKNOWN);
    }
}
