package com.chatbot.chatpush;

import com.chatbot.chatpush.dto.ChatPushConfig;
import com.chatbot.chatpush.dto.ChatPushContext;
import com.chatbot.chatpush.dto.ChatPushStatus;
import com.chatbot.config.BotProperties;
import com.chatbot.delivery.DeliveryMode;
import com.chatbot.delivery.ResponseDeliveryService;
import com.chatbot.memory.ProcessingLockManager;
import com.chatbot.memory.UserMessageQueueStore;
import com.chatbot.repository.ChatMessageRecord;
import com.chatbot.repository.ChatMessageRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ChatPushService {

    private static final Logger log = LoggerFactory.getLogger(ChatPushService.class);
    private static final Duration RECENT_REMINDER_GAP = Duration.ofMinutes(30);

    private final BotProperties botProperties;
    private final ChatPushPolicy chatPushPolicy;
    private final ChatPushConfigCommandHandler configCommandHandler;
    private final ChatPushTopicSource chatPushTopicSource;
    private final ChatPushMessageGenerator chatPushMessageGenerator;
    private final ChatMessageRepository chatMessageRepository;
    private final UserMessageQueueStore userMessageQueueStore;
    private final ProcessingLockManager processingLockManager;
    private final ResponseDeliveryService responseDeliveryService;

    public ChatPushService(BotProperties botProperties,
                           ChatPushPolicy chatPushPolicy,
                           ChatPushConfigCommandHandler configCommandHandler,
                           ChatPushTopicSource chatPushTopicSource,
                           ChatPushMessageGenerator chatPushMessageGenerator,
                           ChatMessageRepository chatMessageRepository,
                           UserMessageQueueStore userMessageQueueStore,
                           ProcessingLockManager processingLockManager,
                           ResponseDeliveryService responseDeliveryService) {
        this.botProperties = botProperties;
        this.chatPushPolicy = chatPushPolicy;
        this.configCommandHandler = configCommandHandler;
        this.chatPushTopicSource = chatPushTopicSource;
        this.chatPushMessageGenerator = chatPushMessageGenerator;
        this.chatMessageRepository = chatMessageRepository;
        this.userMessageQueueStore = userMessageQueueStore;
        this.processingLockManager = processingLockManager;
        this.responseDeliveryService = responseDeliveryService;
    }

    public ChatPushCommandResult handleCommand(Long qqId, String text) {
        String trimmed = text == null ? "" : text.trim();
        return switch (trimmed) {
            case "/chatpush on" -> ChatPushCommandResult.replied(toggleChatPush(qqId, true));
            case "/chatpush off" -> ChatPushCommandResult.replied(toggleChatPush(qqId, false));
            case "/chatpush status", "/chatpush config" -> ChatPushCommandResult.replied(renderStatus(qqId));
            case "/chatpush test" -> {
                ChatPushAttemptResult result = trySendChatPush(qqId, true);
                yield result.sent() ? ChatPushCommandResult.handledWithoutReply() : ChatPushCommandResult.replied(result.message());
            }
            default -> {
                ChatPushCommandResult configResult = configCommandHandler.handle(qqId, trimmed);
                if (!configResult.handled()) {
                    yield ChatPushCommandResult.unhandled();
                }
                if ("/chatpush config".equals(trimmed)) {
                    yield ChatPushCommandResult.replied(renderStatus(qqId));
                }
                yield configResult;
            }
        };
    }

    public ChatPushStatus getStatus(Long qqId) {
        LocalDate today = LocalDate.now(zoneId());
        ChatPushConfig config = chatPushPolicy.getEffectiveConfig(qqId);
        Integer sentToday = null;
        String queueStatus = "未知";
        LocalDateTime lastUserMessageAt = null;
        try {
            sentToday = chatPushPolicy.getDailySentCount(qqId, today);
            queueStatus = isBusy(qqId) ? "忙碌" : "空闲";
            lastUserMessageAt = chatMessageRepository.findLatestByQqIdAndRole(qqId, "user")
                    .map(ChatMessageRecord::createdAt)
                    .orElse(null);
        } catch (Exception ex) {
            log.warn("Failed to build chatpush status for qqId={}", qqId, ex);
        }
        LocalDateTime lastSentAt = resolveLastSentAt(qqId);
        return new ChatPushStatus(
                config.enabled(),
                sentToday,
                config.maxPerDay(),
                config.maxPerDayEnabled(),
                config.minIdleMinutes(),
                config.cooldownMinutes(),
                config.quietEnabled(),
                config.quietStart(),
                config.quietEnd(),
                lastSentAt,
                lastUserMessageAt,
                queueStatus
        );
    }

    public ChatPushAttemptResult trySendChatPush(Long qqId, boolean manualTest) {
        try {
            ChatPushConfig config = chatPushPolicy.getEffectiveConfig(qqId);
            if (!config.enabled()) {
                return ChatPushAttemptResult.skipped(manualTest ? "主动话题还没开启。你可以先发送 /chatpush on。" : null);
            }

            LocalDateTime now = LocalDateTime.now(zoneId());
            if (chatPushPolicy.isQuietTime(qqId, now)) {
                return ChatPushAttemptResult.skipped(manualTest ? "现在在安静时间内，主动话题不会发送。" : null);
            }
            if (isBusy(qqId)) {
                return ChatPushAttemptResult.skipped(manualTest ? "当前队列还在处理消息，等这一轮聊完再试。" : null);
            }

            Integer sentToday = chatPushPolicy.getDailySentCount(qqId, now.toLocalDate());
            if (config.maxPerDayEnabled()) {
                if (sentToday == null) {
                    return ChatPushAttemptResult.skipped(manualTest ? "今日次数状态暂时不可用，先稍后再试。" : null);
                }
                if (sentToday >= config.maxPerDay()) {
                    return ChatPushAttemptResult.skipped(manualTest ? "今天主动话题次数已到上限。" : null);
                }
            }

            Optional<ChatMessageRecord> latestUserMessage = chatMessageRepository.findLatestByQqIdAndRole(qqId, "user");
            if (latestUserMessage.isEmpty()) {
                return ChatPushAttemptResult.skipped(manualTest ? "还没有可参考的聊天记录，先聊几句再试。" : null);
            }
            LocalDateTime idleThreshold = now.minusMinutes(config.minIdleMinutes());
            if (latestUserMessage.get().createdAt().isAfter(idleThreshold)) {
                return ChatPushAttemptResult.skipped(manualTest
                        ? "你刚聊过，至少要空闲 " + formatMinutes(config.minIdleMinutes()) + " 才会主动发起话题。"
                        : null);
            }

            LocalDateTime lastSentAt = resolveLastSentAt(qqId);
            if (lastSentAt != null && lastSentAt.isAfter(now.minusMinutes(config.cooldownMinutes()))) {
                return ChatPushAttemptResult.skipped(manualTest
                        ? "两次主动话题至少要间隔 " + formatMinutes(config.cooldownMinutes()) + "。"
                        : null);
            }

            Optional<ChatMessageRecord> latestReminder = chatMessageRepository.findLatestByQqIdAndSessionId(qqId, "proactive");
            if (latestReminder.isPresent() && latestReminder.get().createdAt().isAfter(now.minus(RECENT_REMINDER_GAP))) {
                return ChatPushAttemptResult.skipped(manualTest ? "刚发过提醒，先别连着打扰。" : null);
            }

            ChatPushContext context = chatPushTopicSource.buildContext(qqId);
            String message = trimToMaxLength(chatPushMessageGenerator.generate(context));
            Long oneBotMessageId = responseDeliveryService.deliver(qqId, message, DeliveryMode.CHATPUSH);
            if (oneBotMessageId == null) {
                return ChatPushAttemptResult.skipped(manualTest ? "主动话题测试失败了，稍后再试。" : null);
            }
            chatMessageRepository.save(qqId, "chatpush", "assistant", message, oneBotMessageId);
            chatPushPolicy.incrementDailySentCount(qqId, now.toLocalDate());
            chatPushPolicy.setLastSentAt(qqId, now.atZone(zoneId()).toInstant());
            return ChatPushAttemptResult.sent(message);
        } catch (Exception ex) {
            log.warn("Failed to send chatpush for qqId={}", qqId, ex);
            return ChatPushAttemptResult.skipped(manualTest ? "主动话题测试失败了，稍后再试。" : null);
        }
    }

    private String renderStatus(Long qqId) {
        ChatPushStatus status = getStatus(qqId);
        String dailyLimitText = status.maxPerDayEnabled() ? String.valueOf(status.maxPerDay()) : "不限制";
        String quietText = status.quietEnabled()
                ? "开启，" + status.quietStart() + " - " + status.quietEnd()
                : "关闭";
        String maxText = status.maxPerDayEnabled()
                ? "开启，最多 " + status.maxPerDay() + " 次"
                : "关闭";
        return "主动话题：" + (status.enabled() ? "开启" : "关闭")
                + "\n今日已主动发起：" + valueOrUnknown(status.sentToday()) + " / " + dailyLimitText
                + "\n最小空闲时间：" + formatMinutes(status.minIdleMinutes())
                + "\n冷却时间：" + formatMinutes(status.cooldownMinutes())
                + "\n安静时间：" + quietText
                + "\n每日次数限制：" + maxText
                + "\n上次主动发起：" + formatTime(status.lastSentAt())
                + "\n上次用户主动聊天：" + formatTime(status.lastUserMessageAt())
                + "\n当前队列状态：" + status.queueStatus();
    }

    private String toggleChatPush(Long qqId, boolean enabled) {
        try {
            chatPushPolicy.setUserEnabled(qqId, enabled);
            if (enabled) {
                return "已开启主动话题。";
            }
            return "已关闭主动话题。提醒功能不受影响。";
        } catch (Exception ex) {
            log.warn("Failed to toggle chatpush for qqId={}", qqId, ex);
            return "配置保存失败了，稍后再试。";
        }
    }

    private boolean isBusy(Long qqId) {
        return userMessageQueueStore.hasMessages(qqId) || processingLockManager.isLocked(qqId);
    }

    private String trimToMaxLength(String message) {
        if (!StringUtils.hasText(message)) {
            return "";
        }
        if (message.length() <= botProperties.getChatpush().getMaxMessageLength()) {
            return message;
        }
        return message.substring(0, botProperties.getChatpush().getMaxMessageLength());
    }

    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "无";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String valueOrUnknown(Integer value) {
        return value == null ? "未知" : value.toString();
    }

    private LocalDateTime resolveLastSentAt(Long qqId) {
        Instant fromRedis = chatPushPolicy.getLastSentAt(qqId);
        if (fromRedis != null) {
            return LocalDateTime.ofInstant(fromRedis, zoneId());
        }
        return chatMessageRepository.findLatestByQqIdAndSessionId(qqId, "chatpush")
                .map(ChatMessageRecord::createdAt)
                .orElse(null);
    }

    private String formatMinutes(int minutes) {
        if (minutes < 60) {
            return minutes + " 分钟";
        }
        if (minutes % 60 == 0) {
            return (minutes / 60) + " 小时";
        }
        int hours = minutes / 60;
        int remain = minutes % 60;
        return hours + " 小时 " + remain + " 分钟";
    }

    private ZoneId zoneId() {
        return chatPushPolicy.zoneId();
    }

    public record ChatPushAttemptResult(boolean sent, String message) {

        static ChatPushAttemptResult sent(String message) {
            return new ChatPushAttemptResult(true, message);
        }

        static ChatPushAttemptResult skipped(String message) {
            return new ChatPushAttemptResult(false, message);
        }
    }
}
