package com.chatbot.memory;

import com.chatbot.chat.QueuedUserMessage;
import java.util.List;

public interface UserMessageQueueStore {

    void enqueue(Long qqId, QueuedUserMessage message);

    List<QueuedUserMessage> drain(Long qqId, int maxBatchSize);

    boolean hasMessages(Long qqId);
}
