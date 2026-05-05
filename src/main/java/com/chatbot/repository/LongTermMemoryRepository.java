package com.chatbot.repository;

import java.util.List;
import java.util.Optional;

public interface LongTermMemoryRepository {

    Long save(String qqId, String memoryKey, String content, Long sourceMessageId, int importance, String qdrantPointId);

    void updateQdrantPointId(Long id, String qdrantPointId);

    List<LongTermMemoryRecord> findEnabledByQqId(String qqId, int limit);

    Optional<LongTermMemoryRecord> findByIdAndQqId(Long id, String qqId);

    void disable(Long id, String qqId);
}
