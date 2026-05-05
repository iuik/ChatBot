package com.chatbot.security;

import com.chatbot.config.BotProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OwnerWhitelist {

    private final BotProperties botProperties;

    public OwnerWhitelist(BotProperties botProperties) {
        this.botProperties = botProperties;
    }

    public boolean isOwner(Long userId) {
        if (userId == null) {
            return false;
        }
        String ownerQq = botProperties.getOwnerQq();
        return StringUtils.hasText(ownerQq) && ownerQq.trim().equals(String.valueOf(userId));
    }
}
