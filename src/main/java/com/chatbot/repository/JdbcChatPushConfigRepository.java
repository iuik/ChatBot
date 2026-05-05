package com.chatbot.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcChatPushConfigRepository implements ChatPushConfigRepository {

    private final JdbcClient jdbcClient;

    public JdbcChatPushConfigRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<ChatPushConfigRecord> findByQqId(String qqId) {
        return jdbcClient.sql("""
                SELECT id, qq_id, enabled, min_idle_minutes, cooldown_minutes, quiet_enabled, quiet_start, quiet_end,
                       max_per_day, max_per_day_enabled, created_at, updated_at
                FROM chatpush_config
                WHERE qq_id = ?
                """)
                .param(qqId)
                .query(this::mapRecord)
                .optional();
    }

    @Override
    public void save(ChatPushConfigRecord record) {
        Integer updated = jdbcClient.sql("""
                UPDATE chatpush_config
                SET enabled = ?, min_idle_minutes = ?, cooldown_minutes = ?, quiet_enabled = ?, quiet_start = ?,
                    quiet_end = ?, max_per_day = ?, max_per_day_enabled = ?
                WHERE qq_id = ?
                """)
                .params(
                        record.enabled(),
                        record.minIdleMinutes(),
                        record.cooldownMinutes(),
                        record.quietEnabled(),
                        record.quietStart(),
                        record.quietEnd(),
                        record.maxPerDay(),
                        record.maxPerDayEnabled(),
                        record.qqId()
                )
                .update();
        if (updated != null && updated > 0) {
            return;
        }
        jdbcClient.sql("""
                INSERT INTO chatpush_config
                (qq_id, enabled, min_idle_minutes, cooldown_minutes, quiet_enabled, quiet_start, quiet_end,
                 max_per_day, max_per_day_enabled)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)
                .params(
                        record.qqId(),
                        record.enabled(),
                        record.minIdleMinutes(),
                        record.cooldownMinutes(),
                        record.quietEnabled(),
                        record.quietStart(),
                        record.quietEnd(),
                        record.maxPerDay(),
                        record.maxPerDayEnabled()
                )
                .update();
    }

    @Override
    public void deleteByQqId(String qqId) {
        jdbcClient.sql("""
                DELETE FROM chatpush_config
                WHERE qq_id = ?
                """)
                .param(qqId)
                .update();
    }

    private ChatPushConfigRecord mapRecord(ResultSet resultSet, int rowNum) throws SQLException {
        return new ChatPushConfigRecord(
                resultSet.getLong("id"),
                resultSet.getString("qq_id"),
                resultSet.getBoolean("enabled"),
                resultSet.getInt("min_idle_minutes"),
                resultSet.getInt("cooldown_minutes"),
                resultSet.getBoolean("quiet_enabled"),
                resultSet.getString("quiet_start"),
                resultSet.getString("quiet_end"),
                resultSet.getInt("max_per_day"),
                resultSet.getBoolean("max_per_day_enabled"),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class)
        );
    }
}
