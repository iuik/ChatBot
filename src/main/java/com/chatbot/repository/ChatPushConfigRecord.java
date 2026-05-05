package com.chatbot.repository;

import java.time.LocalDateTime;

public record ChatPushConfigRecord(
        Long id,
        String qqId,
        boolean enabled,
        int minIdleMinutes,
        int cooldownMinutes,
        boolean quietEnabled,
        String quietStart,
        String quietEnd,
        int maxPerDay,
        boolean maxPerDayEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
