package com.chatbot.repository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository {

    void save(Long qqId, String sessionId, String role, String content, Long onebotMessageId);

    Optional<ChatMessageRecord> findLatestByQqIdAndRole(Long qqId, String role);

    Optional<ChatMessageRecord> findLatestByQqIdAndSessionId(Long qqId, String sessionId);

    List<ChatMessageRecord> findRecentByQqId(Long qqId, int limit);
}
