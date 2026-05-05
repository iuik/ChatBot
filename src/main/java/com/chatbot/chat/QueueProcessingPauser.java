package com.chatbot.chat;

@FunctionalInterface
public interface QueueProcessingPauser {

    void pause(long debounceMillis) throws InterruptedException;
}
