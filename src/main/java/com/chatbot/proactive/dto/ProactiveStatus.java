package com.chatbot.proactive.dto;

public record ProactiveStatus(
        boolean enabled,
        String quietHours,
        Integer sentToday,
        int maxPerDay,
        long pendingTasks
) {
}
