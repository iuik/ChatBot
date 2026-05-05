package com.chatbot.repository;

import java.time.LocalDateTime;

public record ProactiveTaskRecord(
        Long id,
        String qqId,
        String taskType,
        String title,
        String content,
        LocalDateTime scheduledAt,
        LocalDateTime nextRunAt,
        LocalDateTime lastRunAt,
        String status,
        boolean enabled
) {
}
