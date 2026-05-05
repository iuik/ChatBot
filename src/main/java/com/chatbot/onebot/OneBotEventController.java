package com.chatbot.onebot;

import com.chatbot.chat.PrivateMessageService;
import com.chatbot.onebot.dto.OneBotMessageEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OneBotEventController {

    private final PrivateMessageService privateMessageService;

    public OneBotEventController(PrivateMessageService privateMessageService) {
        this.privateMessageService = privateMessageService;
    }

    @PostMapping("/onebot/event")
    public ResponseEntity<Void> receiveEvent(@RequestBody(required = false) OneBotMessageEvent event) {
        privateMessageService.handleMessageEvent(event);
        return ResponseEntity.ok().build();
    }
}
