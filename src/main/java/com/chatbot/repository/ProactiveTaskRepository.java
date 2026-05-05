package com.chatbot.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProactiveTaskRepository {

    Long save(String qqId, String taskType, String title, String content, LocalDateTime scheduledAt, LocalDateTime nextRunAt);

    List<ProactiveTaskRecord> findUpcoming(String qqId, int limit);

    List<ProactiveTaskRecord> findDueTasks(LocalDateTime now, int limit);

    Optional<ProactiveTaskRecord> findByIdAndQqId(Long id, String qqId);

    void cancel(Long id, String qqId);

    void markSent(Long id, LocalDateTime lastRunAt);

    void markFailed(Long id, LocalDateTime lastRunAt);

    void defer(Long id, LocalDateTime nextRunAt);

    long countPending(String qqId);
}
