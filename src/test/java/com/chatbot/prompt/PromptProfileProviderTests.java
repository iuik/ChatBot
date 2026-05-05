package com.chatbot.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.chatbot.config.BotProperties;
import com.chatbot.prompt.dto.PromptProfile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PromptProfileProviderTests {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadProfileFromFileWhenFileFirst() throws Exception {
        Path profileFile = tempDir.resolve("persona.md");
        Files.writeString(profileFile, "file profile", StandardCharsets.UTF_8);

        BotProperties properties = new BotProperties();
        properties.getPrompt().setProfileFile(profileFile.toString());
        properties.getPrompt().setProfileText("env profile");
        properties.getPrompt().setProfileSourcePriority("file-first");

        DefaultPromptProfileProvider provider = new DefaultPromptProfileProvider(properties, new FilePromptProfileProvider());
        provider.initialize();

        PromptProfile profile = provider.getProfile();
        assertThat(profile.source()).isEqualTo(PromptProfile.Source.FILE);
        assertThat(profile.text()).isEqualTo("file profile");
    }

    @Test
    void shouldReadProfileFromEnvWhenEnvFirst() throws Exception {
        Path profileFile = tempDir.resolve("persona.md");
        Files.writeString(profileFile, "file profile", StandardCharsets.UTF_8);

        BotProperties properties = new BotProperties();
        properties.getPrompt().setProfileFile(profileFile.toString());
        properties.getPrompt().setProfileText("env profile");
        properties.getPrompt().setProfileSourcePriority("env-first");

        DefaultPromptProfileProvider provider = new DefaultPromptProfileProvider(properties, new FilePromptProfileProvider());
        provider.initialize();

        PromptProfile profile = provider.getProfile();
        assertThat(profile.source()).isEqualTo(PromptProfile.Source.ENV);
        assertThat(profile.text()).isEqualTo("env profile");
    }

    @Test
    void shouldFallbackToEnvWhenFileMissing() {
        BotProperties properties = new BotProperties();
        properties.getPrompt().setProfileFile(tempDir.resolve("missing.md").toString());
        properties.getPrompt().setProfileText("env profile");
        properties.getPrompt().setProfileSourcePriority("file-first");

        DefaultPromptProfileProvider provider = new DefaultPromptProfileProvider(properties, new FilePromptProfileProvider());
        provider.initialize();

        PromptProfile profile = provider.getProfile();
        assertThat(profile.source()).isEqualTo(PromptProfile.Source.ENV);
        assertThat(profile.text()).isEqualTo("env profile");
    }

    @Test
    void shouldFallbackToDefaultWhenNoProfileConfigured() {
        BotProperties properties = new BotProperties();
        DefaultPromptProfileProvider provider = new DefaultPromptProfileProvider(properties, new FilePromptProfileProvider());
        provider.initialize();

        PromptProfile profile = provider.getProfile();
        assertThat(profile.source()).isEqualTo(PromptProfile.Source.DEFAULT);
        assertThat(profile.text()).contains("QQ 私聊 AI 助手");
    }

    @Test
    void shouldTruncateProfileWhenExceedingLimit() {
        BotProperties properties = new BotProperties();
        properties.getPrompt().setProfileText("1234567890");
        properties.getPrompt().setMaxProfileChars(5);

        DefaultPromptProfileProvider provider = new DefaultPromptProfileProvider(properties, new FilePromptProfileProvider());
        provider.initialize();

        PromptProfile profile = provider.getProfile();
        assertThat(profile.text()).isEqualTo("12345");
        assertThat(profile.truncated()).isTrue();
    }

    @Test
    void shouldReloadFileProfile() throws Exception {
        Path profileFile = tempDir.resolve("persona.md");
        Files.writeString(profileFile, "v1", StandardCharsets.UTF_8);

        BotProperties properties = new BotProperties();
        properties.getPrompt().setProfileFile(profileFile.toString());

        DefaultPromptProfileProvider provider = new DefaultPromptProfileProvider(properties, new FilePromptProfileProvider());
        provider.initialize();
        Files.writeString(profileFile, "v2", StandardCharsets.UTF_8);

        PromptProfile profile = provider.reload();
        assertThat(profile.text()).isEqualTo("v2");
    }
}
