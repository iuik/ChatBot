package com.chatbot.chatpush;

import java.time.Instant;
import java.time.LocalDate;

public interface ChatPushStateStore {

    Boolean getUserEnabled(Long qqId);

    void setUserEnabled(Long qqId, boolean enabled);

    Integer getDailySentCount(Long qqId, LocalDate date);

    void incrementDailySentCount(Long qqId, LocalDate date);

    Instant getLastSentAt(Long qqId);

    void setLastSentAt(Long qqId, Instant instant);
}
