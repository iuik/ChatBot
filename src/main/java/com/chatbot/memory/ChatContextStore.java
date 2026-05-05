package com.chatbot.memory;

import com.chatbot.chat.ChatMessageContext;
import java.util.List;

public interface ChatContextStore {

    void append(Long qqId, ChatMessageContext message);

    List<ChatMessageContext> getRecentMessages(Long qqId, int limit);
}
