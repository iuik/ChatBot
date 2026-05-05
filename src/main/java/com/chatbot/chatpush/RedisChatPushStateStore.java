package com.chatbot.chatpush;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisChatPushStateStore implements ChatPushStateStore {

    private static final Duration DAILY_TTL = Duration.ofDays(2);

    private final StringRedisTemplate redisTemplate;

    public RedisChatPushStateStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Boolean getUserEnabled(Long qqId) {
        String value = redisTemplate.opsForValue().get(enabledKey(qqId));
        return value == null ? null : Boolean.parseBoolean(value);
    }

    @Override
    public void setUserEnabled(Long qqId, boolean enabled) {
        redisTemplate.opsForValue().set(enabledKey(qqId), Boolean.toString(enabled));
    }

    @Override
    public Integer getDailySentCount(Long qqId, LocalDate date) {
        String value = redisTemplate.opsForValue().get(dailyCountKey(qqId, date));
        return value == null ? 0 : Integer.parseInt(value);
    }

    @Override
    public void incrementDailySentCount(Long qqId, LocalDate date) {
        String key = dailyCountKey(qqId, date);
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, DAILY_TTL);
    }

    @Override
    public Instant getLastSentAt(Long qqId) {
        String value = redisTemplate.opsForValue().get(lastSentKey(qqId));
        return value == null ? null : Instant.ofEpochMilli(Long.parseLong(value));
    }

    @Override
    public void setLastSentAt(Long qqId, Instant instant) {
        redisTemplate.opsForValue().set(lastSentKey(qqId), Long.toString(instant.toEpochMilli()));
    }

    private String enabledKey(Long qqId) {
        return "qqbot:chatpush:enabled:" + qqId;
    }

    private String dailyCountKey(Long qqId, LocalDate date) {
        return "qqbot:chatpush:" + qqId + ":date:" + date.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private String lastSentKey(Long qqId) {
        return "qqbot:chatpush:last-sent:" + qqId;
    }
}
