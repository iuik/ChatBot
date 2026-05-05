package com.chatbot.proactive;

import static org.assertj.core.api.Assertions.assertThat;

import com.chatbot.proactive.dto.CreateReminderCommand;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class ReminderTimeParserTests {

    private final ReminderTextNormalizer normalizer = new ReminderTextNormalizer();
    private final ReminderTimeParser parser = new ReminderTimeParser(
            Clock.fixed(Instant.parse("2026-05-03T01:00:00Z"), ZoneId.of("UTC")),
            normalizer
    );

    @Test
    void shouldNormalizeFullWidthColon() {
        assertThat(parser.normalize("22：20")).isEqualTo("22:20");
    }

    @Test
    void shouldParseFullDateTime() {
        CreateReminderCommand command = parser.parse("2026-05-04 09:00 finish project", ZoneId.of("Asia/Shanghai"));
        assertThat(command).isNotNull();
        assertThat(command.scheduledAt().toString()).isEqualTo("2026-05-04T09:00");
    }

    @Test
    void shouldParseTomorrowMorningChineseTime() {
        assertThat(parser.parseDateTimeText("明天早上8点", ZoneId.of("Asia/Shanghai")).toString())
                .isEqualTo("2026-05-04T08:00");
        assertThat(parser.parseDateTimeText("明早8点", ZoneId.of("Asia/Shanghai")).toString())
                .isEqualTo("2026-05-04T08:00");
    }

    @Test
    void shouldParseTodayAfternoon() {
        assertThat(parser.parseDateTimeText("今天下午3点", ZoneId.of("Asia/Shanghai")).toString())
                .isEqualTo("2026-05-03T15:00");
    }

    @Test
    void shouldParseTonightWithFullWidthInput() {
        assertThat(parser.parseDateTimeText("今晚22：20", ZoneId.of("Asia/Shanghai")).toString())
                .isEqualTo("2026-05-03T22:20");
    }

    @Test
    void shouldParseTomorrowKeyword() {
        CreateReminderCommand command = parser.parse("tomorrow 09:00 finish project", ZoneId.of("Asia/Shanghai"));
        assertThat(command).isNotNull();
        assertThat(command.scheduledAt().toString()).isEqualTo("2026-05-04T09:00");
    }

    @Test
    void shouldReturnNullForInvalidFormat() {
        assertThat(parser.parse("later do it", ZoneId.of("Asia/Shanghai"))).isNull();
    }
}
