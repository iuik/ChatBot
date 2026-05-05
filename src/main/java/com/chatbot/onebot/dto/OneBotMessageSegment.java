package com.chatbot.onebot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OneBotMessageSegment {

    private String type;

    @JsonProperty("data")
    private SegmentData data;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public SegmentData getData() {
        return data;
    }

    public void setData(SegmentData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SegmentData {

        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
