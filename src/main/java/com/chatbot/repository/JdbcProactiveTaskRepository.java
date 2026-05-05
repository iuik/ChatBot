package com.chatbot.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProactiveTaskRepository implements ProactiveTaskRepository {

    private final JdbcClient jdbcClient;

    public JdbcProactiveTaskRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Long save(String qqId, String taskType, String title, String content, LocalDateTime scheduledAt, LocalDateTime nextRunAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                INSERT INTO proactive_task
                (qq_id, task_type, title, content, scheduled_at, next_run_at, status, enabled)
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING', 1)
                """)
                .params(qqId, taskType, title, content, scheduledAt, nextRunAt)
                .update(keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create proactive task");
        }
        return key.longValue();
    }

    @Override
    public List<ProactiveTaskRecord> findUpcoming(String qqId, int limit) {
        return jdbcClient.sql("""
                SELECT id, qq_id, task_type, title, content, scheduled_at, next_run_at, last_run_at, status, enabled
                FROM proactive_task
                WHERE qq_id = ? AND enabled = 1 AND status IN ('PENDING', 'DEFERRED')
                ORDER BY next_run_at ASC, id ASC
                LIMIT ?
                """)
                .params(qqId, limit)
                .query(this::mapRecord)
                .list();
    }

    @Override
    public List<ProactiveTaskRecord> findDueTasks(LocalDateTime now, int limit) {
        return jdbcClient.sql("""
                SELECT id, qq_id, task_type, title, content, scheduled_at, next_run_at, last_run_at, status, enabled
                FROM proactive_task
                WHERE enabled = 1 AND status IN ('PENDING', 'DEFERRED') AND next_run_at <= ?
                ORDER BY next_run_at ASC, id ASC
                LIMIT ?
                """)
                .params(now, limit)
                .query(this::mapRecord)
                .list();
    }

    @Override
    public Optional<ProactiveTaskRecord> findByIdAndQqId(Long id, String qqId) {
        return jdbcClient.sql("""
                SELECT id, qq_id, task_type, title, content, scheduled_at, next_run_at, last_run_at, status, enabled
                FROM proactive_task
                WHERE id = ? AND qq_id = ?
                """)
                .params(id, qqId)
                .query(this::mapRecord)
                .optional();
    }

    @Override
    public void cancel(Long id, String qqId) {
        jdbcClient.sql("""
                UPDATE proactive_task
                SET status = 'CANCELLED', enabled = 0
                WHERE id = ? AND qq_id = ?
                """)
                .params(id, qqId)
                .update();
    }

    @Override
    public void markSent(Long id, LocalDateTime lastRunAt) {
        jdbcClient.sql("""
                UPDATE proactive_task
                SET status = 'SENT', enabled = 0, last_run_at = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """)
                .params(lastRunAt, id)
                .update();
    }

    @Override
    public void markFailed(Long id, LocalDateTime lastRunAt) {
        jdbcClient.sql("""
                UPDATE proactive_task
                SET status = 'FAILED', enabled = 0, last_run_at = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """)
                .params(lastRunAt, id)
                .update();
    }

    @Override
    public void defer(Long id, LocalDateTime nextRunAt) {
        jdbcClient.sql("""
                UPDATE proactive_task
                SET status = 'DEFERRED', next_run_at = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """)
                .params(nextRunAt, id)
                .update();
    }

    @Override
    public long countPending(String qqId) {
        Long count = jdbcClient.sql("""
                SELECT COUNT(*)
                FROM proactive_task
                WHERE qq_id = ? AND enabled = 1 AND status IN ('PENDING', 'DEFERRED')
                """)
                .param(qqId)
                .query(Long.class)
                .single();
        return count == null ? 0L : count;
    }

    private ProactiveTaskRecord mapRecord(ResultSet resultSet, int rowNum) throws SQLException {
        return new ProactiveTaskRecord(
                resultSet.getLong("id"),
                resultSet.getString("qq_id"),
                resultSet.getString("task_type"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                resultSet.getObject("scheduled_at", LocalDateTime.class),
                resultSet.getObject("next_run_at", LocalDateTime.class),
                resultSet.getObject("last_run_at", LocalDateTime.class),
                resultSet.getString("status"),
                resultSet.getBoolean("enabled")
        );
    }
}
