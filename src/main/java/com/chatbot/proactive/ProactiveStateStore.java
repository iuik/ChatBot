package com.chatbot.proactive;

import java.time.LocalDate;

public interface ProactiveStateStore {

    boolean isUserEnabled(Long qqId);

    void setUserEnabled(Long qqId, boolean enabled);

    Integer getDailySentCount(Long qqId, LocalDate date);

    long incrementDailySentCount(Long qqId, LocalDate date);
}
