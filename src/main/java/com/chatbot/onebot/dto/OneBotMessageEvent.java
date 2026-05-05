package com.chatbot.onebot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OneBotMessageEvent {

    @JsonProperty("post_type")
    private String postType;

    @JsonProperty("message_type")
    private String messageType;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("message_id")
    private Long messageId;

    @JsonProperty("raw_message")
    private String rawMessage;

    private JsonNode message;

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public JsonNode getMessage() {
        return message;
    }

    public void setMessage(JsonNode message) {
        this.message = message;
    }
}
