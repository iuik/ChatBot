package com.chatbot.chatpush.dto;

import java.time.LocalDateTime;

public record ChatPushStatus(
        boolean enabled,
        Integer sentToday,
        int maxPerDay,
        boolean maxPerDayEnabled,
        int minIdleMinutes,
        int cooldownMinutes,
        boolean quietEnabled,
        String quietStart,
        String quietEnd,
        LocalDateTime lastSentAt,
        LocalDateTime lastUserMessageAt,
        String queueStatus
) {
}
