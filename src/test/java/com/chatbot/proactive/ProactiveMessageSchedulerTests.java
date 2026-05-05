package com.chatbot.proactive;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chatbot.delivery.DeliveryMode;
import com.chatbot.delivery.ResponseDeliveryService;
import com.chatbot.repository.ChatMessageRepository;
import com.chatbot.repository.ProactiveTaskRecord;
import com.chatbot.repository.ProactiveTaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProactiveMessageSchedulerTests {

    private ProactiveTaskRepository repository;
    private ProactiveTaskService service;
    private ProactivePolicy policy;
    private ResponseDeliveryService responseDeliveryService;
    private ChatMessageRepository chatMessageRepository;
    private ProactiveMessageScheduler scheduler;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(ProactiveTaskRepository.class);
        service = org.mockito.Mockito.mock(ProactiveTaskService.class);
        policy = org.mockito.Mockito.mock(ProactivePolicy.class);
        responseDeliveryService = org.mockito.Mockito.mock(ResponseDeliveryService.class);
        chatMessageRepository = org.mockito.Mockito.mock(ChatMessageRepository.class);
        scheduler = new ProactiveMessageScheduler(repository, service, policy, responseDeliveryService, chatMessageRepository);
        when(policy.zoneId()).thenReturn(ZoneId.of("Asia/Shanghai"));
        when(policy.quietHoursEnd()).thenReturn(LocalTime.of(8, 30));
        when(service.deferToAfterQuietHours(any())).thenAnswer(invocation -> ((LocalDateTime) invocation.getArgument(0)).plusMinutes(30));
    }

    @Test
    void schedulerShouldNotSendWhenProactiveOff() {
        ProactiveTaskRecord task = task(1L);
        when(policy.isUserEnabled(123456789L)).thenReturn(false);

        scheduler.processTask(task, LocalDateTime.of(2026, 5, 3, 9, 0));

        verify(responseDeliveryService, never()).deliver(any(), any(), any());
    }

    @Test
    void schedulerShouldDeferWhenQuietHours() {
        ProactiveTaskRecord task = task(1L);
        when(policy.isUserEnabled(123456789L)).thenReturn(true);
        when(policy.isQuietTime(any())).thenReturn(true);

        scheduler.processTask(task, LocalDateTime.of(2026, 5, 3, 23, 40));

        verify(repository).defer(any(), any());
        verify(responseDeliveryService, never()).deliver(any(), any(), any());
    }

    @Test
    void schedulerShouldDeferWhenExceedDailyLimit() {
        ProactiveTaskRecord task = task(1L);
        when(policy.isUserEnabled(123456789L)).thenReturn(true);
        when(policy.isQuietTime(any())).thenReturn(false);
        when(policy.exceededDailyLimit(123456789L, LocalDate.of(2026, 5, 3))).thenReturn(true);

        scheduler.processTask(task, LocalDateTime.of(2026, 5, 3, 9, 0));

        verify(repository).defer(any(), any());
        verify(responseDeliveryService, never()).deliver(any(), any(), any());
    }

    @Test
    void schedulerShouldMarkSentAfterSuccess() {
        ProactiveTaskRecord task = task(1L);
        when(policy.isUserEnabled(123456789L)).thenReturn(true);
        when(policy.isQuietTime(any())).thenReturn(false);
        when(policy.exceededDailyLimit(123456789L, LocalDate.of(2026, 5, 3))).thenReturn(false);
        when(responseDeliveryService.deliver(123456789L, "提醒：do it", DeliveryMode.REMINDER)).thenReturn(8L);

        scheduler.processTask(task, LocalDateTime.of(2026, 5, 3, 9, 0));

        verify(repository).markSent(any(), any());
        verify(chatMessageRepository).save(123456789L, "proactive", "assistant", "提醒：do it", 8L);
    }

    @Test
    void schedulerShouldMarkFailedWhenSendFails() {
        ProactiveTaskRecord task = task(1L);
        when(policy.isUserEnabled(123456789L)).thenReturn(true);
        when(policy.isQuietTime(any())).thenReturn(false);
        when(policy.exceededDailyLimit(123456789L, LocalDate.of(2026, 5, 3))).thenReturn(false);
        when(responseDeliveryService.deliver(123456789L, "提醒：do it", DeliveryMode.REMINDER))
                .thenThrow(new IllegalStateException("send failed"));

        scheduler.processTask(task, LocalDateTime.of(2026, 5, 3, 9, 0));

        verify(repository).markFailed(any(), any());
    }

    private ProactiveTaskRecord task(Long id) {
        LocalDateTime time = LocalDateTime.of(2026, 5, 3, 9, 0);
        return new ProactiveTaskRecord(id, "123456789", "REMINDER", null, "do it", time, time, null, "PENDING", true);
    }
}
