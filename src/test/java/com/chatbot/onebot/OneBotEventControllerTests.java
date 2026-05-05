package com.chatbot.onebot;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chatbot.chat.PrivateMessageService;
import com.chatbot.config.BotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OneBotEventController.class)
@Import(OneBotEventControllerTests.Config.class)
class OneBotEventControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BotProperties botProperties;

    @MockBean
    private PrivateMessageService privateMessageService;

    @BeforeEach
    void setUp() {
        botProperties.getOnebot().setCallbackToken(null);
    }

    @Test
    void shouldAcceptEventWithoutAuthorizationWhenCallbackTokenIsNotConfigured() throws Exception {
        mockMvc.perform(post("/onebot/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(privateMessageService).handleMessageEvent(any());
    }

    @Test
    void shouldAcceptEventWhenBearerTokenMatches() throws Exception {
        botProperties.getOnebot().setCallbackToken("callback-secret");

        mockMvc.perform(post("/onebot/event")
                        .header(AUTHORIZATION, "Bearer callback-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(privateMessageService).handleMessageEvent(any());
    }

    @Test
    void shouldRejectEventWhenBearerTokenDoesNotMatch() throws Exception {
        botProperties.getOnebot().setCallbackToken("callback-secret");

        mockMvc.perform(post("/onebot/event")
                        .header(AUTHORIZATION, "Bearer wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(privateMessageService);
    }

    @Test
    void shouldRejectEventWhenAuthorizationHeaderIsMissing() throws Exception {
        botProperties.getOnebot().setCallbackToken("callback-secret");

        mockMvc.perform(post("/onebot/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(privateMessageService);
    }

    @TestConfiguration
    @EnableConfigurationProperties(BotProperties.class)
    @Import(ValidationAutoConfiguration.class)
    static class Config {
    }
}
