package com.chatbot.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLongTermMemoryRepository implements LongTermMemoryRepository {

    private final JdbcClient jdbcClient;

    public JdbcLongTermMemoryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Long save(String qqId, String memoryKey, String content, Long sourceMessageId, int importance, String qdrantPointId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                INSERT INTO long_term_memory
                (qq_id, memory_key, content, source_message_id, importance, enabled, qdrant_point_id)
                VALUES (?, ?, ?, ?, ?, 1, ?)
                """)
                .params(qqId, memoryKey, content, sourceMessageId, importance, qdrantPointId)
                .update(keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create long term memory row");
        }
        return key.longValue();
    }

    @Override
    public void updateQdrantPointId(Long id, String qdrantPointId) {
        jdbcClient.sql("""
                UPDATE long_term_memory
                SET qdrant_point_id = ?
                WHERE id = ?
                """)
                .params(qdrantPointId, id)
                .update();
    }

    @Override
    public List<LongTermMemoryRecord> findEnabledByQqId(String qqId, int limit) {
        return jdbcClient.sql("""
                SELECT id, qq_id, memory_key, content, source_message_id, importance, enabled, qdrant_point_id,
                       created_at, updated_at
                FROM long_term_memory
                WHERE qq_id = ? AND enabled = 1
                ORDER BY updated_at DESC, id DESC
                LIMIT ?
                """)
                .params(qqId, limit)
                .query(this::mapRecord)
                .list();
    }

    @Override
    public Optional<LongTermMemoryRecord> findByIdAndQqId(Long id, String qqId) {
        return jdbcClient.sql("""
                SELECT id, qq_id, memory_key, content, source_message_id, importance, enabled, qdrant_point_id,
                       created_at, updated_at
                FROM long_term_memory
                WHERE id = ? AND qq_id = ?
                """)
                .params(id, qqId)
                .query(this::mapRecord)
                .optional();
    }

    @Override
    public void disable(Long id, String qqId) {
        jdbcClient.sql("""
                UPDATE long_term_memory
                SET enabled = 0
                WHERE id = ? AND qq_id = ?
                """)
                .params(id, qqId)
                .update();
    }

    private LongTermMemoryRecord mapRecord(ResultSet resultSet, int rowNum) throws SQLException {
        return new LongTermMemoryRecord(
                resultSet.getLong("id"),
                resultSet.getString("qq_id"),
                resultSet.getString("memory_key"),
                resultSet.getString("content"),
                resultSet.getObject("source_message_id", Long.class),
                resultSet.getInt("importance"),
                resultSet.getBoolean("enabled"),
                resultSet.getString("qdrant_point_id"),
                toInstant(resultSet.getTimestamp("created_at")),
                toInstant(resultSet.getTimestamp("updated_at"))
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
