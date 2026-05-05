package com.chatbot.chatpush;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chatbot.chatpush.dto.ChatPushCandidate;
import com.chatbot.chatpush.dto.ChatPushContext;
import com.chatbot.config.BotProperties;
import com.chatbot.delivery.DeliveryMode;
import com.chatbot.delivery.ResponseDeliveryService;
import com.chatbot.memory.ProcessingLockManager;
import com.chatbot.memory.UserMessageQueueStore;
import com.chatbot.repository.ChatMessageRecord;
import com.chatbot.repository.ChatMessageRepository;
import com.chatbot.repository.ChatPushConfigRecord;
import com.chatbot.repository.ChatPushConfigRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChatPushServiceTests {

    private BotProperties botProperties;
    private ChatPushStateStore chatPushStateStore;
    private ChatPushConfigRepository chatPushConfigRepository;
    private ChatPushPolicy chatPushPolicy;
    private ChatPushTopicSource chatPushTopicSource;
    private ChatPushMessageGenerator chatPushMessageGenerator;
    private ChatMessageRepository chatMessageRepository;
    private UserMessageQueueStore userMessageQueueStore;
    private ProcessingLockManager processingLockManager;
    private ResponseDeliveryService responseDeliveryService;
    private ChatPushService service;

    @BeforeEach
    void setUp() {
        botProperties = new BotProperties();
        botProperties.getChatpush().setTimezone("Asia/Shanghai");
        botProperties.getChatpush().setQuietHoursStart("00:00");
        botProperties.getChatpush().setQuietHoursEnd("00:00");
        chatPushStateStore = org.mockito.Mockito.mock(ChatPushStateStore.class);
        chatPushConfigRepository = org.mockito.Mockito.mock(ChatPushConfigRepository.class);
        ChatPushConfigService configService = new ChatPushConfigService(chatPushConfigRepository, botProperties);
        ChatPushConfigParser parser = new ChatPushConfigParser();
        ChatPushConfigCommandHandler configCommandHandler = new ChatPushConfigCommandHandler(parser, configService);
        chatPushPolicy = new ChatPushPolicy(botProperties, configService, chatPushStateStore);
        chatPushTopicSource = org.mockito.Mockito.mock(ChatPushTopicSource.class);
        chatPushMessageGenerator = org.mockito.Mockito.mock(ChatPushMessageGenerator.class);
        chatMessageRepository = org.mockito.Mockito.mock(ChatMessageRepository.class);
        userMessageQueueStore = org.mockito.Mockito.mock(UserMessageQueueStore.class);
        processingLockManager = org.mockito.Mockito.mock(ProcessingLockManager.class);
        responseDeliveryService = org.mockito.Mockito.mock(ResponseDeliveryService.class);
        when(chatPushConfigRepository.findByQqId("123456789")).thenReturn(Optional.empty());
        service = new ChatPushService(
                botProperties,
                chatPushPolicy,
                configCommandHandler,
                chatPushTopicSource,
                chatPushMessageGenerator,
                chatMessageRepository,
                userMessageQueueStore,
                processingLockManager,
                responseDeliveryService
        );
    }

    @Test
    void chatPushOnShouldPersistEnabledConfig() {
        ChatPushCommandResult reply = service.handleCommand(123456789L, "/chatpush on");

        ArgumentCaptor<ChatPushConfigRecord> captor = ArgumentCaptor.forClass(ChatPushConfigRecord.class);
        verify(chatPushConfigRepository).save(captor.capture());
        assertThat(captor.getValue().enabled()).isTrue();
        assertThat(reply.reply()).contains("已开启主动话题");
    }

    @Test
    void chatPushOffShouldPersistDisabledConfig() {
        ChatPushCommandResult reply = service.handleCommand(123456789L, "/chatpush off");

        ArgumentCaptor<ChatPushConfigRecord> captor = ArgumentCaptor.forClass(ChatPushConfigRecord.class);
        verify(chatPushConfigRepository).save(captor.capture());
        assertThat(captor.getValue().enabled()).isFalse();
        assertThat(reply.reply()).contains("已关闭主动话题");
    }

    @Test
    void chatPushSetIdleHoursShouldSaveMinutes() {
        ChatPushCommandResult reply = service.handleCommand(123456789L, "/chatpush set idle 6h");

        ArgumentCaptor<ChatPushConfigRecord> captor = ArgumentCaptor.forClass(ChatPushConfigRecord.class);
        verify(chatPushConfigRepository).save(captor.capture());
        assertThat(captor.getValue().minIdleMinutes()).isEqualTo(360);
        assertThat(reply.reply()).contains("6 小时");
    }

    @Test
    void chatPushSetIdleMinutesShouldSaveMinutes() {
        ChatPushCommandResult reply = service.handleCommand(123456789L, "/chatpush set idle 30m");

        ArgumentCaptor<ChatPushConfigRecord> captor = ArgumentCaptor.forClass(ChatPushConfigRecord.class);
        verify(chatPushConfigRepository).save(captor.capture());
        assertThat(captor.getValue().minIdleMinutes()).isEqualTo(30);
        assertThat(reply.reply()).contains("30 分钟");
    }

    @Test
    void chatPushSetCooldownShouldSaveMinutes() {
        ChatPushCommandResult reply = service.handleCommand(123456789L, "/chatpush set cooldown 2h");

        ArgumentCaptor<ChatPushConfigRecord> captor = ArgumentCaptor.forClass(ChatPushConfigRecord.class);
        verify(chatPushConfigRepository).save(captor.capture());
        assertThat(captor.getValue().cooldownMinutes()).isEqualTo(120);
        assertThat(reply.reply()).contains("2 小时");
    }

    @Test
    void chatPushSetQuietShouldSaveRange() {
        ChatPushCommandResult reply = service.handleCommand(123456789L, "/chatpush set quiet 23:30 08:30");

        ArgumentCaptor<ChatPushConfigRecord> captor = ArgumentCaptor.forClass(ChatPushConfigRecord.class);
        verify(chatPushConfigRepository).save(captor.capture());
        assertThat(captor.getValue().quietStart()).isEqualTo("23:30");
        assertThat(captor.getValue().quietEnd()).isEqualTo("08:30");
        assertThat(captor.getValue().quietEnabled()).isTrue();
        assertThat(reply.reply()).contains("23:30 - 08:30");
    }

    @Test
    void chatPushSetQuietShouldNormalizeFullWidthColon() {
        service.handleCommand(123456789L, "/chatpush set quiet 22：30 08：00");

        ArgumentCaptor<ChatPushConfigRecord> captor = ArgumentCaptor.forClass(ChatPushConfigRecord.class);
        verify(chatPushConfigRepository).save(captor.capture());
        assertThat(captor.getValue().quietStart()).isEqualTo("22:30");
        assertThat(captor.getValue().quietEnd()).isEqualTo("08:00");
    }

    @Test
    void chatPushSetQuietOffShouldDisableQuietHours() {
        ChatPushCommandResult reply = service.handleCommand(123456789L, "/chatpush set quiet off");

        ArgumentCaptor<ChatPushConfigRecord> captor = ArgumentCaptor.forClass(ChatPushConfigRecord.class);
        verify(chatPushConfigRepository).save(captor.capture());
        assertThat(captor.getValue().quietEnabled()).isFalse();
        assertThat(reply.reply()).contains("已关闭安静时间限制");
    }

    @Test
    void chatPushSetQuietOnShouldEnableQuietHours() {
        when(chatPushConfigRepository.findByQqId("123456789"))
                .thenReturn(Optional.of(configRecord(false, 360, 360, false, "23:30", "08:30", 2, true)));

        ChatPushCommandResult reply = service.handleCommand(123456789L, "/chatpush set quiet on");

        ArgumentCaptor<ChatPushConfigRecord> captor = ArgumentCaptor.forClass(ChatPushConfigRecord.class);
        verify(chatPushConfigRepository).save(captor.capture());
        assertThat(captor.getValue().quietEnabled()).isTrue();
        assertThat(reply.reply()).contains("已开启安静时间限制");
    }

    @Test
    void chatPushSetMaxShouldSaveValueAndEnableLimit() {
        ChatPushCommandResult reply = service.handleCommand(123456789L, "/chatpush set max 3");

        ArgumentCaptor<ChatPushConfigRecord> captor = ArgumentCaptor.forClass(ChatPushConfigRecord.class);
        verify(chatPushConfigRepository).save(captor.capture());
        assertThat(captor.getValue().maxPerDay()).isEqualTo(3);
        assertThat(captor.getValue().maxPerDayEnabled()).isTrue();
        assertThat(reply.reply()).contains("3 次");
    }

    @Test
    void chatPushSetMaxOffShouldDisableLimit() {
        ChatPushCommandResult reply = service.handleCommand(123456789L, "/chatpush set max off");

        ArgumentCaptor<ChatPushConfigRecord> captor = ArgumentCaptor.forClass(ChatPushConfigRecord.class);
        verify(chatPushConfigRepository).save(captor.capture());
        assertThat(captor.getValue().maxPerDayEnabled()).isFalse();
        assertThat(reply.reply()).contains("仍会受空闲时间和冷却时间限制");
    }

    @Test
    void chatPushResetShouldDeleteUserConfig() {
        ChatPushCommandResult reply = service.handleCommand(123456789L, "/chatpush reset");

        verify(chatPushConfigRepository).deleteByQqId("123456789");
        assertThat(reply.reply()).contains("已恢复 ChatPush 默认配置");
    }

    @Test
    void chatPushStatusShouldRenderDisabledQuietAndMaxLimit() {
        when(chatPushConfigRepository.findByQqId("123456789"))
                .thenReturn(Optional.of(configRecord(true, 180, 120, false, "23:30", "08:30", 3, false)));
        when(chatPushStateStore.getDailySentCount(eq(123456789L), any(LocalDate.class))).thenReturn(1);
        when(chatPushStateStore.getLastSentAt(123456789L)).thenReturn(Instant.parse("2026-05-03T01:00:00Z"));
        when(userMessageQueueStore.hasMessages(123456789L)).thenReturn(false);
        when(processingLockManager.isLocked(123456789L)).thenReturn(false);
        when(chatMessageRepository.findLatestByQqIdAndRole(123456789L, "user"))
                .thenReturn(Optional.of(chatRecord("user", "hello", LocalDateTime.of(2026, 5, 3, 10, 0), "private:123456789")));

        ChatPushCommandResult reply = service.handleCommand(123456789L, "/chatpush status");

        assertThat(reply.reply()).contains("主动话题：开启");
        assertThat(reply.reply()).contains("今日已主动发起：1 / 不限制");
        assertThat(reply.reply()).contains("最小空闲时间：3 小时");
        assertThat(reply.reply()).contains("冷却时间：2 小时");
        assertThat(reply.reply()).contains("安静时间：关闭");
        assertThat(reply.reply()).contains("每日次数限制：关闭");
    }

    @Test
    void chatPushTestSuccessShouldNotReturnDebugReply() {
        when(chatPushConfigRepository.findByQqId("123456789"))
                .thenReturn(Optional.of(configRecord(true, 30, 30, false, "23:30", "08:30", 2, true)));
        when(chatPushStateStore.getDailySentCount(eq(123456789L), any(LocalDate.class))).thenReturn(0);
        when(chatPushStateStore.getLastSentAt(123456789L)).thenReturn(Instant.parse("2026-05-01T00:00:00Z"));
        when(userMessageQueueStore.hasMessages(123456789L)).thenReturn(false);
        when(processingLockManager.isLocked(123456789L)).thenReturn(false);
        when(chatMessageRepository.findLatestByQqIdAndRole(123456789L, "user"))
                .thenReturn(Optional.of(chatRecord("user", "old", LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusHours(8), "private:123456789")));
        when(chatMessageRepository.findLatestByQqIdAndSessionId(123456789L, "proactive")).thenReturn(Optional.empty());
        when(chatPushTopicSource.buildContext(123456789L)).thenReturn(context("候选话题"));
        when(chatPushMessageGenerator.generate(any())).thenReturn("来继续收尾主动话题模块？");
        when(responseDeliveryService.deliver(123456789L, "来继续收尾主动话题模块？", DeliveryMode.CHATPUSH)).thenReturn(88L);

        ChatPushCommandResult reply = service.handleCommand(123456789L, "/chatpush test");

        assertThat(reply.handled()).isTrue();
        assertThat(reply.reply()).isNull();
        verify(responseDeliveryService).deliver(123456789L, "来继续收尾主动话题模块？", DeliveryMode.CHATPUSH);
        verify(chatMessageRepository).save(123456789L, "chatpush", "assistant", "来继续收尾主动话题模块？", 88L);
        verify(chatPushStateStore).incrementDailySentCount(eq(123456789L), any(LocalDate.class));
        verify(chatPushStateStore).setLastSentAt(eq(123456789L), any(Instant.class));
    }

    @Test
    void chatPushTestFailureShouldReturnFriendlyError() {
        when(chatPushConfigRepository.findByQqId("123456789"))
                .thenReturn(Optional.of(configRecord(true, 30, 30, false, "23:30", "08:30", 2, true)));
        when(chatPushStateStore.getDailySentCount(eq(123456789L), any(LocalDate.class))).thenReturn(0);
        when(chatPushStateStore.getLastSentAt(123456789L)).thenReturn(Instant.parse("2026-05-01T00:00:00Z"));
        when(userMessageQueueStore.hasMessages(123456789L)).thenReturn(false);
        when(processingLockManager.isLocked(123456789L)).thenReturn(false);
        when(chatMessageRepository.findLatestByQqIdAndRole(123456789L, "user"))
                .thenReturn(Optional.of(chatRecord("user", "old", LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusHours(8), "private:123456789")));
        when(chatMessageRepository.findLatestByQqIdAndSessionId(123456789L, "proactive")).thenReturn(Optional.empty());
        when(chatPushTopicSource.buildContext(123456789L)).thenReturn(context("候选话题"));
        when(chatPushMessageGenerator.generate(any())).thenReturn("来继续收尾主动话题模块？");
        when(responseDeliveryService.deliver(123456789L, "来继续收尾主动话题模块？", DeliveryMode.CHATPUSH)).thenReturn(null);

        ChatPushCommandResult reply = service.handleCommand(123456789L, "/chatpush test");

        assertThat(reply.reply()).isEqualTo("主动话题测试失败了，稍后再试。");
    }

    @Test
    void schedulerShouldNotSendWhenChatPushDisabled() {
        ChatPushService.ChatPushAttemptResult result = service.trySendChatPush(123456789L, false);

        assertThat(result.sent()).isFalse();
        verify(responseDeliveryService, never()).deliver(anyLong(), any(), any());
    }

    @Test
    void schedulerShouldNotSendWhenDailyLimitReached() {
        when(chatPushConfigRepository.findByQqId("123456789"))
                .thenReturn(Optional.of(configRecord(true, 360, 360, false, "23:30", "08:30", 2, true)));
        when(chatPushStateStore.getDailySentCount(eq(123456789L), any(LocalDate.class))).thenReturn(2);
        when(userMessageQueueStore.hasMessages(123456789L)).thenReturn(false);
        when(processingLockManager.isLocked(123456789L)).thenReturn(false);

        ChatPushService.ChatPushAttemptResult result = service.trySendChatPush(123456789L, false);

        assertThat(result.sent()).isFalse();
    }

    @Test
    void schedulerShouldSendWhenAllConditionsSatisfied() {
        when(chatPushConfigRepository.findByQqId("123456789"))
                .thenReturn(Optional.of(configRecord(true, 360, 360, false, "23:30", "08:30", 2, true)));
        when(chatPushStateStore.getDailySentCount(eq(123456789L), any(LocalDate.class))).thenReturn(0);
        when(chatPushStateStore.getLastSentAt(123456789L)).thenReturn(Instant.parse("2026-05-02T00:00:00Z"));
        when(userMessageQueueStore.hasMessages(123456789L)).thenReturn(false);
        when(processingLockManager.isLocked(123456789L)).thenReturn(false);
        when(chatMessageRepository.findLatestByQqIdAndRole(123456789L, "user"))
                .thenReturn(Optional.of(chatRecord("user", "old", LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusHours(8), "private:123456789")));
        when(chatMessageRepository.findLatestByQqIdAndSessionId(123456789L, "proactive")).thenReturn(Optional.empty());
        when(chatPushTopicSource.buildContext(123456789L)).thenReturn(context("基于提醒发起话题"));
        when(chatPushMessageGenerator.generate(any())).thenReturn("你之前设了继续写项目的提醒，要不要先拆一步？");
        when(responseDeliveryService.deliver(123456789L, "你之前设了继续写项目的提醒，要不要先拆一步？", DeliveryMode.CHATPUSH)).thenReturn(66L);

        ChatPushService.ChatPushAttemptResult result = service.trySendChatPush(123456789L, false);

        assertThat(result.sent()).isTrue();
        verify(chatMessageRepository).save(123456789L, "chatpush", "assistant", "你之前设了继续写项目的提醒，要不要先拆一步？", 66L);
    }

    private ChatPushContext context(String summary) {
        return new ChatPushContext(
                List.of("QQ AI 助手项目"),
                List.of("user：继续写主动话题"),
                List.of("继续写 QQ AI 项目"),
                new ChatPushCandidate("REMINDER", summary),
                "fallback"
        );
    }

    private ChatMessageRecord chatRecord(String role, String content, LocalDateTime createdAt, String sessionId) {
        return new ChatMessageRecord(1L, 123456789L, sessionId, role, content, null, createdAt);
    }

    private ChatPushConfigRecord configRecord(boolean enabled,
                                              int minIdleMinutes,
                                              int cooldownMinutes,
                                              boolean quietEnabled,
                                              String quietStart,
                                              String quietEnd,
                                              int maxPerDay,
                                              boolean maxPerDayEnabled) {
        return new ChatPushConfigRecord(
                1L,
                "123456789",
                enabled,
                minIdleMinutes,
                cooldownMinutes,
                quietEnabled,
                quietStart,
                quietEnd,
                maxPerDay,
                maxPerDayEnabled,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
