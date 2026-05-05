package com.chatbot.chat;

import java.time.Instant;

public record QueuedUserMessage(
        Long qqId,
        Long messageId,
        String content,
        Instant createdAt
) {
}
