package com.chatbot.chatpush;

public record ChatPushCommandResult(boolean handled, String reply) {

    public static ChatPushCommandResult unhandled() {
        return new ChatPushCommandResult(false, null);
    }

    public static ChatPushCommandResult replied(String reply) {
        return new ChatPushCommandResult(true, reply);
    }

    public static ChatPushCommandResult handledWithoutReply() {
        return new ChatPushCommandResult(true, null);
    }
}
