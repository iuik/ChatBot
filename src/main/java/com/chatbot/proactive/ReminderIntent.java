package com.chatbot.proactive;

public record ReminderIntent(
        ReminderIntentType type,
        String timeText,
        String content,
        double confidence,
        String originalText
) {

    public static ReminderIntent unknown(String originalText) {
        return new ReminderIntent(ReminderIntentType.UNKNOWN, null, null, 0.0, originalText);
    }
}
