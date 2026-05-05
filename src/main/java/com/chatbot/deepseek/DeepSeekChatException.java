package com.chatbot.deepseek;

public class DeepSeekChatException extends RuntimeException {

    public DeepSeekChatException(String message) {
        super(message);
    }

    public DeepSeekChatException(String message, Throwable cause) {
        super(message, cause);
    }
}
