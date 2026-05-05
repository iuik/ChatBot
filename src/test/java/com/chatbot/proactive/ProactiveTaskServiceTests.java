package com.chatbot.proactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chatbot.config.BotProperties;
import com.chatbot.repository.ProactiveTaskRecord;
import com.chatbot.repository.ProactiveTaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProactiveTaskServiceTests {

    private ProactiveTaskRepository repository;
    private NaturalReminderIntentDetector detector;
    private ProactiveTaskService service;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(ProactiveTaskRepository.class);
        ProactiveStateStore stateStore = org.mockito.Mockito.mock(ProactiveStateStore.class);
        BotProperties botProperties = new BotProperties();
        botProperties.getProactive().setTimezone("Asia/Shanghai");
        botProperties.getReminder().setAiIntentEnabled(true);
        ProactivePolicy proactivePolicy = new ProactivePolicy(botProperties, stateStore);
        ReminderTextNormalizer normalizer = new ReminderTextNormalizer();
        ReminderTimeParser parser = new ReminderTimeParser(
                Clock.fixed(Instant.parse("2026-05-03T01:00:00Z"), ZoneId.of("UTC")),
                normalizer
        );
        detector = org.mockito.Mockito.mock(NaturalReminderIntentDetector.class);
        ReminderReplyService replyService = new ReminderReplyService(botProperties, null);
        service = new ProactiveTaskService(repository, parser, proactivePolicy, botProperties, detector, replyService);
    }

    @Test
    void remindWithFullTimeShouldCreateTask() {
        when(repository.save(anyString(), any(), any(), anyString(), any(), any())).thenReturn(1L);

        String reply = service.handleCommand(123456789L, "/remind 2026-05-04 09:00 finish project");

        assertThat(reply).contains("已创建提醒 1");
    }

    @Test
    void naturalCreateShouldSaveReminder() {
        when(repository.save(anyString(), any(), any(), anyString(), any(), any())).thenReturn(2L);
        String reply = service.handleCommand(123456789L, "/remind tomorrow 08:00 叫我起床");

        verify(repository).save(anyString(), any(), any(), anyString(), any(), any());
        assertThat(reply).contains("已创建提醒 2");
    }

    @Test
    void naturalCreateShouldSupportRemindMePattern() {
        when(repository.save(anyString(), any(), any(), anyString(), any(), any())).thenReturn(3L);
        String reply = service.handleCommand(123456789L, "/remind tomorrow 09:00 继续写项目");

        verify(repository).save(anyString(), any(), any(), anyString(), any(), any());
        assertThat(reply).contains("已创建提醒 3");
    }

    @Test
    void ordinaryQuestionShouldNotCreateReminder() {
        when(detector.detect("明天早上8点我应该做什么？"))
                .thenReturn(ReminderIntent.unknown("明天早上8点我应该做什么？"));
        String reply = service.handleNaturalMessage(123456789L, "明天早上8点我应该做什么？");

        assertThat(reply).isNull();
        verify(repository, never()).save(anyString(), any(), any(), anyString(), any(), any());
    }

    @Test
    void opinionQuestionShouldNotCreateReminder() {
        when(detector.detect("你觉得我明天要不要起床？"))
                .thenReturn(ReminderIntent.unknown("你觉得我明天要不要起床？"));
        String reply = service.handleNaturalMessage(123456789L, "你觉得我明天要不要起床？");

        assertThat(reply).isNull();
        verify(repository, never()).save(anyString(), any(), any(), anyString(), any(), any());
    }

    @Test
    void remindersShouldListTasks() {
        when(repository.findUpcoming("123456789", 10)).thenReturn(List.of(
                record(1L, "task", LocalDateTime.of(2026, 5, 4, 9, 0))
        ));

        String reply = service.handleCommand(123456789L, "/reminders");

        assertThat(reply).contains("1.");
        assertThat(reply).contains("task");
    }

    @Test
    void remindDeleteShouldCancelTask() {
        when(repository.findByIdAndQqId(1L, "123456789")).thenReturn(Optional.of(record(1L, "task", LocalDateTime.now())));

        String reply = service.handleCommand(123456789L, "/remind del 1");

        verify(repository).cancel(1L, "123456789");
        assertThat(reply).contains("已取消提醒 1");
    }

    @Test
    void naturalDeleteShouldCancelMatchedTask() {
        when(repository.findUpcoming("123456789", 100)).thenReturn(List.of(
                record(1L, "叫我起床", LocalDateTime.of(2026, 5, 4, 8, 0))
        ));
        when(detector.detect("把明天早上8点叫我起床的提醒删了"))
                .thenReturn(new ReminderIntent(ReminderIntentType.DELETE_REMINDER, "tomorrow 08:00", "叫我起床", 0.95, "把明天早上8点叫我起床的提醒删了"));

        String reply = service.handleNaturalMessage(123456789L, "把明天早上8点叫我起床的提醒删了");

        verify(repository).cancel(1L, "123456789");
        assertThat(reply).contains("已经取消");
    }

    @Test
    void naturalDeleteShouldReturnCandidatesWhenAmbiguous() {
        when(repository.findUpcoming("123456789", 100)).thenReturn(List.of(
                record(1L, "叫我起床", LocalDateTime.of(2026, 5, 4, 8, 0)),
                record(2L, "起床后跑步", LocalDateTime.of(2026, 5, 4, 8, 0))
        ));
        when(detector.detect("把明天早上8点的提醒删了"))
                .thenReturn(new ReminderIntent(ReminderIntentType.DELETE_REMINDER, "tomorrow 08:00", null, 0.95, "把明天早上8点的提醒删了"));

        String reply = service.handleNaturalMessage(123456789L, "把明天早上8点的提醒删了");

        verify(repository, never()).cancel(any(), anyString());
        assertThat(reply).contains("你要删哪一个");
        assertThat(reply).contains("/remind del");
    }

    @Test
    void naturalDeleteShouldReturnNotFoundWhenNoTaskMatched() {
        when(repository.findUpcoming("123456789", 100)).thenReturn(List.of(
                record(1L, "叫我起床", LocalDateTime.of(2026, 5, 5, 8, 0))
        ));
        when(detector.detect("把明天早上8点叫我起床的提醒删了"))
                .thenReturn(new ReminderIntent(ReminderIntentType.DELETE_REMINDER, "tomorrow 08:00", "叫我起床", 0.95, "把明天早上8点叫我起床的提醒删了"));

        String reply = service.handleNaturalMessage(123456789L, "把明天早上8点叫我起床的提醒删了");

        assertThat(reply).contains("我没找到对应的提醒");
    }

    @Test
    void invalidRemindShouldReturnUsage() {
        String reply = service.handleCommand(123456789L, "/remind later");
        assertThat(reply).contains("/remind 2026-05-04 09:00");
    }

    private ProactiveTaskRecord record(Long id, String content, LocalDateTime time) {
        return new ProactiveTaskRecord(id, "123456789", "REMINDER", null, content, time, time, null, "PENDING", true);
    }
}
