package com.chatbot.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chatbot.chat.ChatMessageContext;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisChatContextStore implements ChatContextStore {

    private static final TypeReference<List<ChatMessageContext>> MESSAGE_LIST_TYPE = new TypeReference<>() {
    };

    private static final int CONTEXT_TTL_DAYS = 7;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatContextStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(Long qqId, ChatMessageContext message) {
        String key = buildKey(qqId);
        List<ChatMessageContext> messages = new ArrayList<>(getRecentMessages(qqId, Integer.MAX_VALUE));
        messages.add(message);
        writeMessages(key, messages);
    }

    @Override
    public List<ChatMessageContext> getRecentMessages(Long qqId, int limit) {
        String serialized = redisTemplate.opsForValue().get(buildKey(qqId));
        if (serialized == null) {
            return List.of();
        }

        try {
            List<ChatMessageContext> messages = objectMapper.readValue(serialized, MESSAGE_LIST_TYPE);
            if (messages.isEmpty()) {
                return List.of();
            }
            int fromIndex = Math.max(0, messages.size() - Math.max(limit, 0));
            return List.copyOf(messages.subList(fromIndex, messages.size()));
        } catch (JsonProcessingException ex) {
            redisTemplate.delete(buildKey(qqId));
            return List.of();
        }
    }

    private void writeMessages(String key, List<ChatMessageContext> messages) {
        List<ChatMessageContext> latestMessages;
        if (messages.size() <= 20) {
            latestMessages = messages;
        } else {
            latestMessages = messages.subList(messages.size() - 20, messages.size());
        }

        try {
            redisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(latestMessages),
                    Duration.ofDays(CONTEXT_TTL_DAYS)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize Redis chat context", ex);
        }
    }

    private String buildKey(Long qqId) {
        return "qqbot:ctx:" + qqId;
    }
}
