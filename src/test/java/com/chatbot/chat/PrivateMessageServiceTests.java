package com.chatbot.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chatbot.chatpush.ChatPushCommandResult;
import com.chatbot.chatpush.ChatPushService;
import com.chatbot.delivery.DeliveryMode;
import com.chatbot.delivery.ResponseDeliveryService;
import com.chatbot.memory.ChatContextStore;
import com.chatbot.memory.LongTermMemoryService;
import com.chatbot.memory.MessageDeduplicator;
import com.chatbot.memory.UserMessageQueueStore;
import com.chatbot.onebot.dto.OneBotMessageEvent;
import com.chatbot.prompt.PromptAdminService;
import com.chatbot.proactive.ProactiveTaskService;
import com.chatbot.repository.ChatMessageRepository;
import com.chatbot.security.OwnerWhitelist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PrivateMessageServiceTests {

    private OwnerWhitelist ownerWhitelist;
    private ChatMessageRepository chatMessageRepository;
    private ChatContextStore chatContextStore;
    private MessageDeduplicator messageDeduplicator;
    private UserMessageQueueStore userMessageQueueStore;
    private UserMessageQueueProcessor userMessageQueueProcessor;
    private ChatPushService chatPushService;
    private PromptAdminService promptAdminService;
    private ProactiveTaskService proactiveTaskService;
    private LongTermMemoryService longTermMemoryService;
    private ResponseDeliveryService responseDeliveryService;
    private PrivateMessageService privateMessageService;

    @BeforeEach
    void setUp() {
        ownerWhitelist = org.mockito.Mockito.mock(OwnerWhitelist.class);
        chatMessageRepository = org.mockito.Mockito.mock(ChatMessageRepository.class);
        chatContextStore = org.mockito.Mockito.mock(ChatContextStore.class);
        messageDeduplicator = org.mockito.Mockito.mock(MessageDeduplicator.class);
        userMessageQueueStore = org.mockito.Mockito.mock(UserMessageQueueStore.class);
        userMessageQueueProcessor = org.mockito.Mockito.mock(UserMessageQueueProcessor.class);
        chatPushService = org.mockito.Mockito.mock(ChatPushService.class);
        promptAdminService = org.mockito.Mockito.mock(PromptAdminService.class);
        proactiveTaskService = org.mockito.Mockito.mock(ProactiveTaskService.class);
        longTermMemoryService = org.mockito.Mockito.mock(LongTermMemoryService.class);
        responseDeliveryService = org.mockito.Mockito.mock(ResponseDeliveryService.class);

        when(chatPushService.handleCommand(anyLong(), anyString())).thenReturn(ChatPushCommandResult.unhandled());

        privateMessageService = new PrivateMessageService(
                ownerWhitelist,
                new ObjectMapper(),
                chatMessageRepository,
                chatContextStore,
                messageDeduplicator,
                userMessageQueueStore,
                userMessageQueueProcessor,
                chatPushService,
                promptAdminService,
                proactiveTaskService,
                longTermMemoryService,
                responseDeliveryService
        );
    }

    @Test
    void shouldEnqueueFirstMessageAndStartProcessor() {
        OneBotMessageEvent event = createPrivateTextEvent("hello", 10001L);
        when(ownerWhitelist.isOwner(123456789L)).thenReturn(true);
        when(messageDeduplicator.isDuplicate(10001L)).thenReturn(false);

        privateMessageService.handleMessageEvent(event);

        verify(chatMessageRepository).save(123456789L, "private:123456789", "user", "hello", 10001L);
        verify(chatContextStore).append(123456789L, new ChatMessageContext("user", "hello"));
        verify(userMessageQueueStore).enqueue(anyLong(), any(QueuedUserMessage.class));
        verify(userMessageQueueProcessor).tryStartProcessing(123456789L);
    }

    @Test
    void shouldHandleMemoryCommandWithoutQueueing() {
        OneBotMessageEvent event = createPrivateTextEvent("/memory", 10001L);
        when(ownerWhitelist.isOwner(123456789L)).thenReturn(true);
        when(messageDeduplicator.isDuplicate(10001L)).thenReturn(false);
        when(longTermMemoryService.handleCommand(123456789L, "/memory")).thenReturn("memory list");

        privateMessageService.handleMessageEvent(event);

        verify(responseDeliveryService).deliver(123456789L, "memory list", DeliveryMode.MEMORY);
        verify(userMessageQueueStore, never()).enqueue(anyLong(), any());
        verify(chatMessageRepository, never()).save(anyLong(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void shouldHandleExplicitRemindCommandWithoutQueueing() {
        OneBotMessageEvent event = createPrivateTextEvent("/reminders", 10001L);
        when(ownerWhitelist.isOwner(123456789L)).thenReturn(true);
        when(messageDeduplicator.isDuplicate(10001L)).thenReturn(false);
        when(proactiveTaskService.handleCommand(123456789L, "/reminders")).thenReturn("list");

        privateMessageService.handleMessageEvent(event);

        verify(responseDeliveryService).deliver(123456789L, "list", DeliveryMode.COMMAND);
        verify(userMessageQueueStore, never()).enqueue(anyLong(), any());
        verify(chatMessageRepository, never()).save(anyLong(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void shouldHandlePromptStatusWithoutQueueing() {
        OneBotMessageEvent event = createPrivateTextEvent("/prompt status", 10001L);
        when(ownerWhitelist.isOwner(123456789L)).thenReturn(true);
        when(messageDeduplicator.isDuplicate(10001L)).thenReturn(false);
        when(promptAdminService.handleCommand("/prompt status")).thenReturn("Prompt status");

        privateMessageService.handleMessageEvent(event);

        verify(responseDeliveryService).deliver(123456789L, "Prompt status", DeliveryMode.PROMPT_ADMIN);
        verify(userMessageQueueStore, never()).enqueue(anyLong(), any());
        verify(chatMessageRepository, never()).save(anyLong(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void shouldHandleChatPushCommandWithoutQueueing() {
        OneBotMessageEvent event = createPrivateTextEvent("/chatpush status", 10001L);
        when(ownerWhitelist.isOwner(123456789L)).thenReturn(true);
        when(messageDeduplicator.isDuplicate(10001L)).thenReturn(false);
        when(chatPushService.handleCommand(123456789L, "/chatpush status")).thenReturn(ChatPushCommandResult.replied("status"));

        privateMessageService.handleMessageEvent(event);

        verify(responseDeliveryService).deliver(123456789L, "status", DeliveryMode.COMMAND);
        verify(userMessageQueueStore, never()).enqueue(anyLong(), any());
        verify(chatMessageRepository, never()).save(anyLong(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void shouldHandleChatPushTestWithoutSecondReply() {
        OneBotMessageEvent event = createPrivateTextEvent("/chatpush test", 10001L);
        when(ownerWhitelist.isOwner(123456789L)).thenReturn(true);
        when(messageDeduplicator.isDuplicate(10001L)).thenReturn(false);
        when(chatPushService.handleCommand(123456789L, "/chatpush test")).thenReturn(ChatPushCommandResult.handledWithoutReply());

        privateMessageService.handleMessageEvent(event);

        verify(responseDeliveryService, never()).deliver(anyLong(), anyString(), any());
        verify(userMessageQueueStore, never()).enqueue(anyLong(), any());
    }

    @Test
    void shouldHandleNaturalReminderWithoutQueueing() {
        String text = "你明天早上8点记得叫我起床";
        String reply = "好，明天早上 8 点我叫你起床。";
        OneBotMessageEvent event = createPrivateTextEvent(text, 10001L);
        when(ownerWhitelist.isOwner(123456789L)).thenReturn(true);
        when(messageDeduplicator.isDuplicate(10001L)).thenReturn(false);
        when(proactiveTaskService.handleNaturalMessage(123456789L, text)).thenReturn(reply);

        privateMessageService.handleMessageEvent(event);

        verify(responseDeliveryService).deliver(123456789L, reply, DeliveryMode.REMINDER);
        verify(userMessageQueueStore, never()).enqueue(anyLong(), any());
        verify(chatMessageRepository, never()).save(anyLong(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void shouldFallbackToSynchronousProcessingWhenQueueFails() {
        OneBotMessageEvent event = createPrivateTextEvent("hello", 10001L);
        when(ownerWhitelist.isOwner(123456789L)).thenReturn(true);
        when(messageDeduplicator.isDuplicate(10001L)).thenReturn(false);
        org.mockito.Mockito.doThrow(new IllegalStateException("redis down"))
                .when(userMessageQueueStore).enqueue(anyLong(), any(QueuedUserMessage.class));

        privateMessageService.handleMessageEvent(event);

        verify(userMessageQueueProcessor).processSingleMessage(anyLong(), anyString(), any(QueuedUserMessage.class));
    }

    @Test
    void shouldIgnoreDuplicateMessage() {
        OneBotMessageEvent event = createPrivateTextEvent("hello", 10001L);
        when(ownerWhitelist.isOwner(123456789L)).thenReturn(true);
        when(messageDeduplicator.isDuplicate(10001L)).thenReturn(true);

        privateMessageService.handleMessageEvent(event);

        verify(userMessageQueueStore, never()).enqueue(anyLong(), any());
        verify(chatMessageRepository, never()).save(anyLong(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void shouldIgnoreNonOwnerRemindCommand() {
        OneBotMessageEvent event = createPrivateTextEvent("你明天早上8点记得叫我起床", 10001L);
        when(ownerWhitelist.isOwner(123456789L)).thenReturn(false);

        privateMessageService.handleMessageEvent(event);

        verify(proactiveTaskService, never()).handleCommand(anyLong(), anyString());
        verify(chatPushService, never()).handleCommand(anyLong(), anyString());
        verify(promptAdminService, never()).handleCommand(anyString());
        verify(proactiveTaskService, never()).handleNaturalMessage(anyLong(), anyString());
        verify(longTermMemoryService, never()).handleCommand(anyLong(), anyString());
        verify(responseDeliveryService, never()).deliver(anyLong(), anyString(), any());
    }

    @Test
    void shouldIgnoreNonOwnerChatPushCommand() {
        OneBotMessageEvent event = createPrivateTextEvent("/chatpush on", 10002L);
        when(ownerWhitelist.isOwner(123456789L)).thenReturn(false);

        privateMessageService.handleMessageEvent(event);

        verify(chatPushService, never()).handleCommand(anyLong(), anyString());
        verify(promptAdminService, never()).handleCommand(anyString());
        verify(responseDeliveryService, never()).deliver(anyLong(), anyString(), any());
    }

    private OneBotMessageEvent createPrivateTextEvent(String rawMessage, Long messageId) {
        OneBotMessageEvent event = new OneBotMessageEvent();
        event.setPostType("message");
        event.setMessageType("private");
        event.setUserId(123456789L);
        event.setMessageId(messageId);
        event.setRawMessage(rawMessage);
        return event;
    }
}
