package com.chatbot.memory;

public interface ProcessingLockManager {

    String tryLock(Long qqId);

    void unlock(Long qqId, String token);

    boolean isLocked(Long qqId);
}
