package com.chatbot.repository;

import java.util.Optional;

public interface ChatPushConfigRepository {

    Optional<ChatPushConfigRecord> findByQqId(String qqId);

    void save(ChatPushConfigRecord record);

    void deleteByQqId(String qqId);
}
