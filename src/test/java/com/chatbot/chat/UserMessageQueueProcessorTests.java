package com.chatbot.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chatbot.config.BotProperties;
import com.chatbot.delivery.DeliveryMode;
import com.chatbot.delivery.ResponseDeliveryService;
import com.chatbot.deepseek.DeepSeekChatException;
import com.chatbot.deepseek.DeepSeekChatService;
import com.chatbot.memory.ChatContextStore;
import com.chatbot.memory.LongTermMemoryService;
import com.chatbot.memory.MemoryRetriever;
import com.chatbot.memory.ProcessingLockManager;
import com.chatbot.memory.UserMessageQueueStore;
import com.chatbot.memory.dto.RetrievedMemory;
import com.chatbot.repository.ChatMessageRepository;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserMessageQueueProcessorTests {

    private static final String FALLBACK_REPLY = "模型响应失败了，稍后再试。";

    private ChatContextStore chatContextStore;
    private ChatMessageRepository chatMessageRepository;
    private DeepSeekChatService deepSeekChatService;
    private ResponseDeliveryService responseDeliveryService;
    private MemoryRetriever memoryRetriever;
    private LongTermMemoryService longTermMemoryService;
    private TestUserMessageQueueStore userMessageQueueStore;
    private TestProcessingLockManager processingLockManager;
    private UserMessageQueueProcessor userMessageQueueProcessor;

    @BeforeEach
    void setUp() {
        chatContextStore = org.mockito.Mockito.mock(ChatContextStore.class);
        chatMessageRepository = org.mockito.Mockito.mock(ChatMessageRepository.class);
        deepSeekChatService = org.mockito.Mockito.mock(DeepSeekChatService.class);
        responseDeliveryService = org.mockito.Mockito.mock(ResponseDeliveryService.class);
        memoryRetriever = org.mockito.Mockito.mock(MemoryRetriever.class);
        longTermMemoryService = org.mockito.Mockito.mock(LongTermMemoryService.class);
        userMessageQueueStore = new TestUserMessageQueueStore();
        processingLockManager = new TestProcessingLockManager();

        BotProperties botProperties = new BotProperties();
        botProperties.getChat().setMaxContextMessages(20);
        botProperties.getChat().getQueue().setDebounceMillis(1);
        botProperties.getChat().getQueue().setProcessingLockSeconds(120);
        botProperties.getChat().getQueue().setMaxBatchSize(8);
        botProperties.getChat().getQueue().setMaxMergedMessageChars(4000);

        Executor directExecutor = Runnable::run;
        QueueProcessingPauser noOpPauser = debounceMillis -> {
        };

        userMessageQueueProcessor = new UserMessageQueueProcessor(
                chatContextStore,
                userMessageQueueStore,
                processingLockManager,
                chatMessageRepository,
                deepSeekChatService,
                responseDeliveryService,
                memoryRetriever,
                longTermMemoryService,
                noOpPauser,
                directExecutor,
                botProperties
        );
    }

    @Test
    void shouldMergeMultipleMessagesIntoSingleDeepSeekCall() {
        userMessageQueueStore.enqueue(123456789L, message(123456789L, 1L, "first"));
        userMessageQueueStore.enqueue(123456789L, message(123456789L, 2L, "second"));
        when(chatContextStore.getRecentMessages(123456789L, 20)).thenReturn(List.of(
                new ChatMessageContext("assistant", "old reply"),
                new ChatMessageContext("user", "first"),
                new ChatMessageContext("user", "second")
        ));
        when(memoryRetriever.retrieveRelevantMemories(123456789L, "用户连续发送了多条消息：\n1. first\n2. second\n"))
                .thenReturn(List.of(new RetrievedMemory(1L, "remembered", 0.9)));
        when(deepSeekChatService.chat(any(), any(), any())).thenReturn("assistant reply");
        when(responseDeliveryService.deliver(123456789L, "assistant reply", DeliveryMode.DAILY_CHAT)).thenReturn(900L);

        boolean started = userMessageQueueProcessor.tryStartProcessing(123456789L);

        assertThat(started).isTrue();
        ArgumentCaptor<String> mergedCaptor = ArgumentCaptor.forClass(String.class);
        verify(deepSeekChatService).chat(any(), mergedCaptor.capture(), any());
        assertThat(mergedCaptor.getValue()).contains("1. first");
        assertThat(mergedCaptor.getValue()).contains("2. second");
        verify(memoryRetriever).retrieveRelevantMemories(123456789L, mergedCaptor.getValue());
        verify(responseDeliveryService).deliver(123456789L, "assistant reply", DeliveryMode.DAILY_CHAT);
        verify(chatMessageRepository).save(123456789L, "private:123456789", "assistant", "assistant reply", 900L);
    }

    @Test
    void shouldUseOriginalMessageWhenBatchHasSingleItem() {
        userMessageQueueStore.enqueue(123456789L, message(123456789L, 1L, "single message"));
        when(chatContextStore.getRecentMessages(123456789L, 20)).thenReturn(List.of(
                new ChatMessageContext("user", "single message")
        ));
        when(memoryRetriever.retrieveRelevantMemories(123456789L, "single message")).thenReturn(List.of());
        when(deepSeekChatService.chat(any(), any(), any())).thenReturn("assistant reply");

        userMessageQueueProcessor.tryStartProcessing(123456789L);

        ArgumentCaptor<String> mergedCaptor = ArgumentCaptor.forClass(String.class);
        verify(deepSeekChatService).chat(any(), mergedCaptor.capture(), any());
        assertThat(mergedCaptor.getValue()).isEqualTo("single message");
        assertThat(mergedCaptor.getValue()).doesNotContain("用户连续发送了多条消息");
    }

    @Test
    void shouldSendFallbackOnlyOnceWhenDeepSeekFails() {
        userMessageQueueStore.enqueue(123456789L, message(123456789L, 1L, "hello"));
        when(chatContextStore.getRecentMessages(123456789L, 20)).thenReturn(List.of(
                new ChatMessageContext("user", "hello")
        ));
        when(memoryRetriever.retrieveRelevantMemories(123456789L, "hello")).thenReturn(List.of());
        when(deepSeekChatService.chat(any(), any(), any())).thenThrow(new DeepSeekChatException("timeout"));

        userMessageQueueProcessor.tryStartProcessing(123456789L);

        verify(responseDeliveryService).deliver(123456789L, FALLBACK_REPLY, DeliveryMode.TECHNICAL);
        verify(chatMessageRepository, never()).save(eq(123456789L), eq("private:123456789"), eq("assistant"), any(), any());
    }

    @Test
    void shouldReleaseProcessingLockWhenWorkerFinishes() {
        userMessageQueueStore.enqueue(123456789L, message(123456789L, 1L, "hello"));
        when(chatContextStore.getRecentMessages(123456789L, 20)).thenReturn(List.of(
                new ChatMessageContext("user", "hello")
        ));
        when(memoryRetriever.retrieveRelevantMemories(123456789L, "hello")).thenReturn(List.of());
        when(deepSeekChatService.chat(any(), any(), any())).thenReturn("assistant reply");

        userMessageQueueProcessor.tryStartProcessing(123456789L);

        assertThat(processingLockManager.unlockCount).isEqualTo(1);
    }

    @Test
    void shouldRestartProcessingWhenNewMessageArrivesDuringUnlockRace() {
        userMessageQueueStore.enqueue(123456789L, message(123456789L, 1L, "first"));
        when(chatContextStore.getRecentMessages(123456789L, 20)).thenReturn(List.of(
                new ChatMessageContext("user", "first")
        ));
        when(memoryRetriever.retrieveRelevantMemories(eq(123456789L), any())).thenReturn(List.of());
        when(deepSeekChatService.chat(any(), any(), any()))
                .thenReturn("reply-1")
                .thenReturn("reply-2");

        processingLockManager.onUnlock = () -> {
            if (!processingLockManager.injected) {
                processingLockManager.injected = true;
                userMessageQueueStore.enqueue(123456789L, message(123456789L, 2L, "second"));
            }
        };

        userMessageQueueProcessor.tryStartProcessing(123456789L);

        verify(deepSeekChatService, times(2)).chat(any(), any(), any());
        verify(responseDeliveryService).deliver(123456789L, "reply-1", DeliveryMode.DAILY_CHAT);
        verify(responseDeliveryService).deliver(123456789L, "reply-2", DeliveryMode.DAILY_CHAT);
    }

    @Test
    void shouldUseMergedMessageForMemoryRetrieval() {
        userMessageQueueStore.enqueue(123456789L, message(123456789L, 1L, "first"));
        userMessageQueueStore.enqueue(123456789L, message(123456789L, 2L, "second"));
        when(chatContextStore.getRecentMessages(123456789L, 20)).thenReturn(List.of(
                new ChatMessageContext("user", "first"),
                new ChatMessageContext("user", "second")
        ));
        when(memoryRetriever.retrieveRelevantMemories(eq(123456789L), any())).thenReturn(List.of());
        when(deepSeekChatService.chat(any(), any(), any())).thenReturn("assistant reply");

        userMessageQueueProcessor.tryStartProcessing(123456789L);

        ArgumentCaptor<String> mergedCaptor = ArgumentCaptor.forClass(String.class);
        verify(memoryRetriever).retrieveRelevantMemories(eq(123456789L), mergedCaptor.capture());
        assertThat(mergedCaptor.getValue()).contains("1. first");
        assertThat(mergedCaptor.getValue()).contains("2. second");
    }

    private QueuedUserMessage message(Long qqId, Long messageId, String content) {
        return new QueuedUserMessage(qqId, messageId, content, Instant.now());
    }

    private static final class TestUserMessageQueueStore implements UserMessageQueueStore {

        private final Deque<QueuedUserMessage> queue = new ArrayDeque<>();

        @Override
        public void enqueue(Long qqId, QueuedUserMessage message) {
            queue.addLast(message);
        }

        @Override
        public List<QueuedUserMessage> drain(Long qqId, int maxBatchSize) {
            List<QueuedUserMessage> messages = new ArrayList<>();
            for (int i = 0; i < maxBatchSize; i++) {
                QueuedUserMessage message = queue.pollFirst();
                if (message == null) {
                    break;
                }
                messages.add(message);
            }
            return List.copyOf(messages);
        }

        @Override
        public boolean hasMessages(Long qqId) {
            return !queue.isEmpty();
        }
    }

    private static final class TestProcessingLockManager implements ProcessingLockManager {

        private boolean locked;
        private int unlockCount;
        private Runnable onUnlock;
        private boolean injected;

        @Override
        public String tryLock(Long qqId) {
            if (locked) {
                return null;
            }
            locked = true;
            return "token";
        }

        @Override
        public void unlock(Long qqId, String token) {
            unlockCount++;
            locked = false;
            if (onUnlock != null) {
                onUnlock.run();
            }
        }

        @Override
        public boolean isLocked(Long qqId) {
            return locked;
        }
    }
}
