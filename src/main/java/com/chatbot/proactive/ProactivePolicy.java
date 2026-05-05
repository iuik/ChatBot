package com.chatbot.proactive;

import com.chatbot.config.BotProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProactivePolicy {

    private static final Logger log = LoggerFactory.getLogger(ProactivePolicy.class);

    private final BotProperties botProperties;
    private final ProactiveStateStore proactiveStateStore;

    public ProactivePolicy(BotProperties botProperties, ProactiveStateStore proactiveStateStore) {
        this.botProperties = botProperties;
        this.proactiveStateStore = proactiveStateStore;
    }

    public boolean isGloballyEnabled() {
        return botProperties.getProactive().isEnabled();
    }

    public boolean isUserEnabled(Long qqId) {
        try {
            return proactiveStateStore.isUserEnabled(qqId);
        } catch (Exception ex) {
            log.warn("Failed to read proactive user setting for qqId={}", qqId, ex);
            return true;
        }
    }

    public void setUserEnabled(Long qqId, boolean enabled) {
        proactiveStateStore.setUserEnabled(qqId, enabled);
    }

    public boolean isQuietTime(LocalDateTime now) {
        LocalTime start = LocalTime.parse(botProperties.getProactive().getQuietHoursStart());
        LocalTime end = LocalTime.parse(botProperties.getProactive().getQuietHoursEnd());
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
        try {
            Integer sent = proactiveStateStore.getDailySentCount(qqId, date);
            return sent != null && sent >= botProperties.getProactive().getMaxPerDay();
        } catch (Exception ex) {
            log.warn("Failed to read proactive daily limit count for qqId={}", qqId, ex);
            return false;
        }
    }

    public Integer getDailySentCount(Long qqId, LocalDate date) {
        try {
            return proactiveStateStore.getDailySentCount(qqId, date);
        } catch (Exception ex) {
            log.warn("Failed to get proactive daily count for qqId={}", qqId, ex);
            return null;
        }
    }

    public void incrementDailySentCount(Long qqId, LocalDate date) {
        try {
            proactiveStateStore.incrementDailySentCount(qqId, date);
        } catch (Exception ex) {
            log.warn("Failed to increment proactive daily count for qqId={}", qqId, ex);
        }
    }

    public ZoneId zoneId() {
        return botProperties.proactiveZoneId();
    }

    public LocalTime quietHoursEnd() {
        return LocalTime.parse(botProperties.getProactive().getQuietHoursEnd());
    }

    public String quietHoursText() {
        return botProperties.getProactive().getQuietHoursStart() + " - " + botProperties.getProactive().getQuietHoursEnd();
    }
}
