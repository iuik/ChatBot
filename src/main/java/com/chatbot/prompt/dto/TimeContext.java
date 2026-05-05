package com.chatbot.prompt.dto;

import java.time.LocalDateTime;

public record TimeContext(
        LocalDateTime currentDateTime,
        String timezone,
        String chineseDateText,
        String formattedBlock
) {
}
