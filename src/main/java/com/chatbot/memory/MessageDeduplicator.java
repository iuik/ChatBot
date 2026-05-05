package com.chatbot.memory;

public interface MessageDeduplicator {

    boolean isDuplicate(Long messageId);
}
