package com.chatbot.proactive.dto;

import java.time.LocalDateTime;

public record ProactiveTask(
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
