package com.chatbot.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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
import com.chatbot.onebot.dto.OneBotMessageSegment;
import com.chatbot.prompt.PromptAdminService;
import com.chatbot.proactive.ProactiveTaskService;
import com.chatbot.repository.ChatMessageRepository;
import com.chatbot.security.OwnerWhitelist;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PrivateMessageService {

    private static final Logger log = LoggerFactory.getLogger(PrivateMessageService.class);

    private static final TypeReference<List<OneBotMessageSegment>> SEGMENT_LIST_TYPE = new TypeReference<>() {
    };

    private final OwnerWhitelist ownerWhitelist;
    private final ObjectMapper objectMapper;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatContextStore chatContextStore;
    private final MessageDeduplicator messageDeduplicator;
    private final UserMessageQueueStore userMessageQueueStore;
    private final UserMessageQueueProcessor userMessageQueueProcessor;
    private final ChatPushService chatPushService;
    private final PromptAdminService promptAdminService;
    private final ProactiveTaskService proactiveTaskService;
    private final LongTermMemoryService longTermMemoryService;
    private final ResponseDeliveryService responseDeliveryService;

    public PrivateMessageService(OwnerWhitelist ownerWhitelist,
                                 ObjectMapper objectMapper,
                                 ChatMessageRepository chatMessageRepository,
                                 ChatContextStore chatContextStore,
                                 MessageDeduplicator messageDeduplicator,
                                 UserMessageQueueStore userMessageQueueStore,
                                 UserMessageQueueProcessor userMessageQueueProcessor,
                                 ChatPushService chatPushService,
                                 PromptAdminService promptAdminService,
                                 ProactiveTaskService proactiveTaskService,
                                 LongTermMemoryService longTermMemoryService,
                                 ResponseDeliveryService responseDeliveryService) {
        this.ownerWhitelist = ownerWhitelist;
        this.objectMapper = objectMapper;
        this.chatMessageRepository = chatMessageRepository;
        this.chatContextStore = chatContextStore;
        this.messageDeduplicator = messageDeduplicator;
        this.userMessageQueueStore = userMessageQueueStore;
        this.userMessageQueueProcessor = userMessageQueueProcessor;
        this.chatPushService = chatPushService;
        this.promptAdminService = promptAdminService;
        this.proactiveTaskService = proactiveTaskService;
        this.longTermMemoryService = longTermMemoryService;
        this.responseDeliveryService = responseDeliveryService;
    }

    public void handleMessageEvent(OneBotMessageEvent event) {
        if (event == null) {
            return;
        }
        if (!"message".equals(event.getPostType())) {
            return;
        }
        if (!"private".equals(event.getMessageType())) {
            return;
        }
        if (!ownerWhitelist.isOwner(event.getUserId())) {
            return;
        }

        String text = extractPureText(event);
        if (!StringUtils.hasText(text)) {
            return;
        }

        if (messageDeduplicator.isDuplicate(event.getMessageId())) {
            log.info("Ignored duplicate messageId={}", event.getMessageId());
            return;
        }

        String sessionId = buildSessionId(event.getUserId());
        QueuedUserMessage queuedUserMessage = new QueuedUserMessage(
                event.getUserId(),
                event.getMessageId(),
                text,
                Instant.now()
        );

        ChatPushCommandResult chatPushResult = chatPushService.handleCommand(event.getUserId(), text);
        if (chatPushResult.handled()) {
            if (StringUtils.hasText(chatPushResult.reply())) {
                responseDeliveryService.deliver(event.getUserId(), chatPushResult.reply(), DeliveryMode.COMMAND);
            }
            return;
        }

        String promptReply = promptAdminService.handleCommand(text);
        if (promptReply != null) {
            responseDeliveryService.deliver(event.getUserId(), promptReply, DeliveryMode.PROMPT_ADMIN);
            return;
        }

        String proactiveReply = proactiveTaskService.handleCommand(event.getUserId(), text);
        if (proactiveReply != null) {
            responseDeliveryService.deliver(event.getUserId(), proactiveReply, DeliveryMode.COMMAND);
            return;
        }

        String naturalReminderReply = proactiveTaskService.handleNaturalMessage(event.getUserId(), text);
        if (naturalReminderReply != null) {
            responseDeliveryService.deliver(event.getUserId(), naturalReminderReply, DeliveryMode.REMINDER);
            return;
        }

        String commandReply = longTermMemoryService.handleCommand(event.getUserId(), text);
        if (commandReply != null) {
            responseDeliveryService.deliver(event.getUserId(), commandReply, DeliveryMode.MEMORY);
            return;
        }

        chatMessageRepository.save(event.getUserId(), sessionId, "user", text, event.getMessageId());
        chatContextStore.append(event.getUserId(), new ChatMessageContext("user", text));

        try {
            userMessageQueueStore.enqueue(event.getUserId(), queuedUserMessage);
            userMessageQueueProcessor.tryStartProcessing(event.getUserId());
        } catch (Exception ex) {
            log.error("Failed to enqueue message for qqId={}, falling back to synchronous processing", event.getUserId(), ex);
            userMessageQueueProcessor.processSingleMessage(event.getUserId(), sessionId, queuedUserMessage);
        }
    }

    private String extractPureText(OneBotMessageEvent event) {
        JsonNode messageNode = event.getMessage();
        if (messageNode != null) {
            if (messageNode.isArray()) {
                List<OneBotMessageSegment> segments = objectMapper.convertValue(messageNode, SEGMENT_LIST_TYPE);
                if (segments.isEmpty()) {
                    return null;
                }

                StringBuilder builder = new StringBuilder();
                for (OneBotMessageSegment segment : segments) {
                    if (segment == null || !"text".equals(segment.getType())) {
                        return null;
                    }
                    OneBotMessageSegment.SegmentData data = segment.getData();
                    if (data == null || data.getText() == null) {
                        return null;
                    }
                    builder.append(data.getText());
                }
                return builder.toString();
            }

            if (messageNode.isTextual()) {
                return sanitizePlainText(messageNode.asText());
            }
        }

        return sanitizePlainText(event.getRawMessage());
    }

    private String sanitizePlainText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        if (text.contains("[CQ:")) {
            return null;
        }
        return text;
    }

    private String buildSessionId(Long qqId) {
        return "private:" + qqId;
    }
}
