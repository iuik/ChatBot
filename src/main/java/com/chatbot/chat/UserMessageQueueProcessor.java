package com.chatbot.chat;

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
import com.chatbot.onebot.OneBotApiClient;
import com.chatbot.repository.ChatMessageRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class UserMessageQueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(UserMessageQueueProcessor.class);

    private static final String FALLBACK_REPLY =
            "\u6a21\u578b\u54cd\u5e94\u5931\u8d25\u4e86\uff0c\u7a0d\u540e\u518d\u8bd5\u3002";
    private static final String MULTI_MESSAGE_PREFIX =
            "\u7528\u6237\u8fde\u7eed\u53d1\u9001\u4e86\u591a\u6761\u6d88\u606f\uff1a\n";

    private final ChatContextStore chatContextStore;
    private final UserMessageQueueStore userMessageQueueStore;
    private final ProcessingLockManager processingLockManager;
    private final ChatMessageRepository chatMessageRepository;
    private final DeepSeekChatService deepSeekChatService;
    private final ResponseDeliveryService responseDeliveryService;
    private final MemoryRetriever memoryRetriever;
    private final LongTermMemoryService longTermMemoryService;
    private final QueueProcessingPauser queueProcessingPauser;
    private final Executor chatQueueTaskExecutor;
    private final int maxContextMessages;
    private final long debounceMillis;
    private final int maxBatchSize;
    private final int maxMergedMessageChars;

    public UserMessageQueueProcessor(ChatContextStore chatContextStore,
                                     UserMessageQueueStore userMessageQueueStore,
                                     ProcessingLockManager processingLockManager,
                                     ChatMessageRepository chatMessageRepository,
                                     DeepSeekChatService deepSeekChatService,
                                     ResponseDeliveryService responseDeliveryService,
                                     MemoryRetriever memoryRetriever,
                                     LongTermMemoryService longTermMemoryService,
                                     QueueProcessingPauser queueProcessingPauser,
                                     @Qualifier("chatQueueTaskExecutor") Executor chatQueueTaskExecutor,
                                     BotProperties botProperties) {
        this.chatContextStore = chatContextStore;
        this.userMessageQueueStore = userMessageQueueStore;
        this.processingLockManager = processingLockManager;
        this.chatMessageRepository = chatMessageRepository;
        this.deepSeekChatService = deepSeekChatService;
        this.responseDeliveryService = responseDeliveryService;
        this.memoryRetriever = memoryRetriever;
        this.longTermMemoryService = longTermMemoryService;
        this.queueProcessingPauser = queueProcessingPauser;
        this.chatQueueTaskExecutor = chatQueueTaskExecutor;
        this.maxContextMessages = botProperties.getChat().getMaxContextMessages();
        this.debounceMillis = botProperties.getChat().getQueue().getDebounceMillis();
        this.maxBatchSize = botProperties.getChat().getQueue().getMaxBatchSize();
        this.maxMergedMessageChars = botProperties.getChat().getQueue().getMaxMergedMessageChars();
    }

    public boolean tryStartProcessing(Long qqId) {
        String lockToken = processingLockManager.tryLock(qqId);
        if (lockToken == null) {
            return false;
        }
        chatQueueTaskExecutor.execute(() -> processQueue(qqId, lockToken));
        return true;
    }

    public void processSingleMessage(Long qqId, String sessionId, QueuedUserMessage message) {
        processBatch(qqId, sessionId, List.of(message));
    }

    void processQueue(Long qqId, String lockToken) {
        try {
            while (true) {
                pauseBeforeDrain();
                List<QueuedUserMessage> batch = userMessageQueueStore.drain(qqId, maxBatchSize);
                if (batch.isEmpty()) {
                    break;
                }
                processBatch(qqId, buildSessionId(qqId), batch);
            }
        } finally {
            processingLockManager.unlock(qqId, lockToken);
            tryRestartIfNeeded(qqId);
        }
    }

    private void processBatch(Long qqId, String sessionId, List<QueuedUserMessage> batch) {
        String mergedUserMessage = mergeMessages(batch);
        List<ChatMessageContext> recentContext = chatContextStore.getRecentMessages(qqId, maxContextMessages);
        List<ChatMessageContext> promptContext = trimTrailingBatchMessages(recentContext, batch.size());
        List<RetrievedMemory> retrievedMemories = memoryRetriever.retrieveRelevantMemories(qqId, mergedUserMessage);

        try {
            String reply = deepSeekChatService.chat(promptContext, mergedUserMessage, retrievedMemories);
            Long assistantMessageId = responseDeliveryService.deliver(qqId, reply, DeliveryMode.DAILY_CHAT);
            chatMessageRepository.save(qqId, sessionId, "assistant", reply, assistantMessageId);
            chatContextStore.append(qqId, new ChatMessageContext("assistant", reply));
            longTermMemoryService.extractAndStore(qqId, mergedUserMessage, batch.get(0).messageId());
        } catch (DeepSeekChatException ex) {
            responseDeliveryService.deliver(qqId, FALLBACK_REPLY, DeliveryMode.TECHNICAL);
        } catch (Exception ex) {
            log.error("Failed to process queued messages for qqId={}", qqId, ex);
            responseDeliveryService.deliver(qqId, FALLBACK_REPLY, DeliveryMode.TECHNICAL);
        }
    }

    private void pauseBeforeDrain() {
        try {
            queueProcessingPauser.pause(debounceMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Queue worker interrupted", ex);
        }
    }

    private void tryRestartIfNeeded(Long qqId) {
        try {
            if (userMessageQueueStore.hasMessages(qqId)) {
                tryStartProcessing(qqId);
            }
        } catch (Exception ex) {
            log.error("Failed to restart queue processor for qqId={}", qqId, ex);
        }
    }

    private String mergeMessages(List<QueuedUserMessage> batch) {
        if (batch.size() == 1) {
            return truncate(batch.get(0).content());
        }

        StringBuilder builder = new StringBuilder(MULTI_MESSAGE_PREFIX);
        for (int i = 0; i < batch.size(); i++) {
            String nextLine = (i + 1) + ". " + batch.get(i).content() + "\n";
            if (builder.length() + nextLine.length() > maxMergedMessageChars) {
                int remaining = maxMergedMessageChars - builder.length();
                if (remaining > 0) {
                    builder.append(nextLine, 0, Math.min(nextLine.length(), remaining));
                }
                return builder.toString();
            }
            builder.append(nextLine);
        }
        return builder.toString();
    }

    private String truncate(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxMergedMessageChars) {
            return content;
        }
        return content.substring(0, maxMergedMessageChars);
    }

    private List<ChatMessageContext> trimTrailingBatchMessages(List<ChatMessageContext> contextMessages, int batchSize) {
        List<ChatMessageContext> trimmed = new ArrayList<>(contextMessages);
        int remainingToTrim = batchSize;
        while (remainingToTrim > 0 && !trimmed.isEmpty()) {
            ChatMessageContext lastMessage = trimmed.get(trimmed.size() - 1);
            if (!"user".equals(lastMessage.role())) {
                break;
            }
            trimmed.remove(trimmed.size() - 1);
            remainingToTrim--;
        }
        return List.copyOf(trimmed);
    }

    private String buildSessionId(Long qqId) {
        return "private:" + qqId;
    }
}
