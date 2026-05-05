package com.chatbot.chatpush.dto;

import java.util.List;

public record ChatPushContext(
        List<String> memories,
        List<String> recentMessages,
        List<String> pendingTasks,
        ChatPushCandidate candidate,
        String fallbackMessage
) {
}
