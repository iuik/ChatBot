package com.chatbot.chatpush;

import com.chatbot.chatpush.dto.ChatPushConfig;
import com.chatbot.config.BotProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ChatPushPolicy {

    private static final Logger log = LoggerFactory.getLogger(ChatPushPolicy.class);

    private final BotProperties botProperties;
    private final ChatPushConfigService chatPushConfigService;
    private final ChatPushStateStore chatPushStateStore;

    public ChatPushPolicy(BotProperties botProperties,
                          ChatPushConfigService chatPushConfigService,
                          ChatPushStateStore chatPushStateStore) {
        this.botProperties = botProperties;
        this.chatPushConfigService = chatPushConfigService;
        this.chatPushStateStore = chatPushStateStore;
    }

    public boolean isEnabled(Long qqId) {
        try {
            return getEffectiveConfig(qqId).enabled();
        } catch (Exception ex) {
            log.warn("Failed to read chatpush enabled state for qqId={}", qqId, ex);
            return false;
        }
    }

    public void setUserEnabled(Long qqId, boolean enabled) {
        chatPushConfigService.updateEnabled(qqId, enabled);
    }

    public ChatPushConfig getEffectiveConfig(Long qqId) {
        return chatPushConfigService.getEffectiveConfig(qqId);
    }

    public boolean isQuietTime(Long qqId, LocalDateTime now) {
        ChatPushConfig config = getEffectiveConfig(qqId);
        if (!config.quietEnabled()) {
            return false;
        }
        LocalTime start = LocalTime.parse(config.quietStart());
        LocalTime end = LocalTime.parse(config.quietEnd());
        LocalTime current = now.toLocalTime();
        if (start.equals(end)) {
            return false;
        }
        if (start.isBefore(end)) {
            return !current.isBefore(start) && current.isBefore(end);
        }
        return !current.isBefore(start) || current.isBefore(end);
    }

    public boolean exceededDailyLimit(Long qqId, LocalDate date) {
        Integer count = getDailySentCount(qqId, date);
        ChatPushConfig config = getEffectiveConfig(qqId);
        return config.maxPerDayEnabled() && count != null && count >= config.maxPerDay();
    }

    public Integer getDailySentCount(Long qqId, LocalDate date) {
        try {
            return chatPushStateStore.getDailySentCount(qqId, date);
        } catch (Exception ex) {
            log.warn("Failed to read chatpush daily count for qqId={}", qqId, ex);
            return null;
        }
    }

    public void incrementDailySentCount(Long qqId, LocalDate date) {
        try {
            chatPushStateStore.incrementDailySentCount(qqId, date);
        } catch (Exception ex) {
            log.warn("Failed to increment chatpush daily count for qqId={}", qqId, ex);
        }
    }

    public Instant getLastSentAt(Long qqId) {
        try {
            return chatPushStateStore.getLastSentAt(qqId);
        } catch (Exception ex) {
            log.warn("Failed to read chatpush last sent for qqId={}", qqId, ex);
            return null;
        }
    }

    public void setLastSentAt(Long qqId, Instant instant) {
        try {
            chatPushStateStore.setLastSentAt(qqId, instant);
        } catch (Exception ex) {
            log.warn("Failed to store chatpush last sent for qqId={}", qqId, ex);
        }
    }

    public ZoneId zoneId() {
        return botProperties.chatPushZoneId();
    }

    public String quietHoursText(Long qqId) {
        ChatPushConfig config = getEffectiveConfig(qqId);
        return config.quietStart() + " - " + config.quietEnd();
    }
}
