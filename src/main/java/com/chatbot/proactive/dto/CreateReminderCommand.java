package com.chatbot.proactive.dto;

import java.time.LocalDateTime;

public record CreateReminderCommand(
        LocalDateTime scheduledAt,
        String content
) {
}
