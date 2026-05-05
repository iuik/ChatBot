package com.chatbot.onebot;

import com.chatbot.chat.PrivateMessageService;
import com.chatbot.config.BotProperties;
import com.chatbot.onebot.dto.OneBotMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class OneBotEventController {

    private static final Logger log = LoggerFactory.getLogger(OneBotEventController.class);

    private final PrivateMessageService privateMessageService;
    private final BotProperties botProperties;

    public OneBotEventController(PrivateMessageService privateMessageService, BotProperties botProperties) {
        this.privateMessageService = privateMessageService;
        this.botProperties = botProperties;
    }

    @PostMapping("/onebot/event")
    public ResponseEntity<Void> receiveEvent(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) OneBotMessageEvent event) {
        validateCallbackToken(authorization);
        privateMessageService.handleMessageEvent(event);
        return ResponseEntity.ok().build();
    }

    private void validateCallbackToken(String authorization) {
        String expectedToken = botProperties.getOnebot().getCallbackToken();
        if (!StringUtils.hasText(expectedToken)) {
            return;
        }

        String actualToken = extractBearerToken(authorization);
        if (!expectedToken.equals(actualToken)) {
            log.warn("Rejected OneBot callback due to invalid authorization header");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OneBot callback token");
        }
    }

    private String extractBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        String trimmed = authorization.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmed.substring(7).trim();
        }
        return trimmed;
    }
}
