package com.chatbot.prompt;

import com.chatbot.prompt.dto.PromptProfile;

public interface PromptProfileProvider {

    PromptProfile getProfile();

    PromptProfile reload();
}
