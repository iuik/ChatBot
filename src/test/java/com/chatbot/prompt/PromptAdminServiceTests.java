package com.chatbot.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.chatbot.config.BotProperties;
import com.chatbot.prompt.dto.PromptProfile;
import org.junit.jupiter.api.Test;

class PromptAdminServiceTests {

    @Test
    void statusShouldNotExposePromptContent() {
        BotProperties properties = new BotProperties();
        PromptAdminService service = new PromptAdminService(properties, new FixedPromptProfileProvider());

        String reply = service.handleCommand("/prompt status");

        assertThat(reply).contains("profile 来源：env");
        assertThat(reply).doesNotContain("secret persona");
    }

    @Test
    void reloadShouldShortCircuitWhenAutoReloadEnabled() {
        BotProperties properties = new BotProperties();
        properties.getPrompt().setReloadFileEachRequest(true);
        PromptAdminService service = new PromptAdminService(properties, new FixedPromptProfileProvider());

        String reply = service.handleCommand("/prompt reload");

        assertThat(reply).contains("无需手动 reload");
    }

    private static class FixedPromptProfileProvider implements PromptProfileProvider {

        @Override
        public PromptProfile getProfile() {
            return new PromptProfile(PromptProfile.Source.ENV, "secret persona", 14, false);
        }

        @Override
        public PromptProfile reload() {
            return getProfile();
        }
    }
}
