package com.chatbot.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcChatMessageRepository implements ChatMessageRepository {

    private final JdbcClient jdbcClient;

    public JdbcChatMessageRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void save(Long qqId, String sessionId, String role, String content, Long onebotMessageId) {
        jdbcClient.sql("""
                INSERT INTO chat_message (qq_id, session_id, role, content, onebot_message_id)
                VALUES (?, ?, ?, ?, ?)
                """)
                .params(qqId, sessionId, role, content, onebotMessageId)
                .update();
    }

    @Override
    public Optional<ChatMessageRecord> findLatestByQqIdAndRole(Long qqId, String role) {
        return jdbcClient.sql("""
                SELECT id, qq_id, session_id, role, content, onebot_message_id, created_at
                FROM chat_message
                WHERE qq_id = ? AND role = ?
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """)
                .params(qqId, role)
                .query(this::mapRecord)
                .optional();
    }

    @Override
    public Optional<ChatMessageRecord> findLatestByQqIdAndSessionId(Long qqId, String sessionId) {
        return jdbcClient.sql("""
                SELECT id, qq_id, session_id, role, content, onebot_message_id, created_at
                FROM chat_message
                WHERE qq_id = ? AND session_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """)
                .params(qqId, sessionId)
                .query(this::mapRecord)
                .optional();
    }

    @Override
    public List<ChatMessageRecord> findRecentByQqId(Long qqId, int limit) {
        return jdbcClient.sql("""
                SELECT id, qq_id, session_id, role, content, onebot_message_id, created_at
                FROM chat_message
                WHERE qq_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """)
                .params(qqId, limit)
                .query(this::mapRecord)
                .list();
    }

    private ChatMessageRecord mapRecord(ResultSet resultSet, int rowNum) throws SQLException {
        return new ChatMessageRecord(
                resultSet.getLong("id"),
                resultSet.getLong("qq_id"),
                resultSet.getString("session_id"),
                resultSet.getString("role"),
                resultSet.getString("content"),
                resultSet.getObject("onebot_message_id", Long.class),
                resultSet.getObject("created_at", LocalDateTime.class)
        );
    }
}
