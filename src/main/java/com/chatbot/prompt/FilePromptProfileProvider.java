package com.chatbot.prompt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Component;

@Component
public class FilePromptProfileProvider {

    public String read(String path) throws IOException {
        Path filePath = Paths.get(path);
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }
}
