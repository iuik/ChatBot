package com.chatbot.onebot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OneBotSendPrivateMessageRequest {

    @JsonProperty("user_id")
    private Long userId;

    private String message;

    public OneBotSendPrivateMessageRequest() {
    }

    public OneBotSendPrivateMessageRequest(Long userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
