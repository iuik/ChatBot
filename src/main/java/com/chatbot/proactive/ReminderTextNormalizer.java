package com.chatbot.proactive;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReminderTextNormalizer {

    public String normalize(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        StringBuilder builder = new StringBuilder(text.length());
        for (char ch : text.toCharArray()) {
            builder.append(normalizeChar(ch));
        }
        String normalized = builder.toString()
                .replace("明早", "明天早上")
                .replace("明晚", "明天晚上")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private char normalizeChar(char ch) {
        if (ch == '\u3000') {
            return ' ';
        }
        if (ch == '\uff1a') {
            return ':';
        }
        if (ch >= '\uff10' && ch <= '\uff19') {
            return (char) ('0' + (ch - '\uff10'));
        }
        return ch;
    }
}
