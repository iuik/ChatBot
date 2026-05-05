package com.chatbot.repository;

import java.time.LocalDateTime;

public record ChatMessageRecord(
        Long id,
        Long qqId,
        String sessionId,
        String role,
        String content,
        Long onebotMessageId,
        LocalDateTime createdAt
) {
}
