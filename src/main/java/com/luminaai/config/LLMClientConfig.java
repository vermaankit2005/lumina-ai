package com.luminaai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

@Configuration
public class LLMClientConfig {

    @Bean
    @Primary
    public ChatClient.Builder chatClientBuilder(Environment env, OpenAiChatModel openAiChatModel, OllamaChatModel ollamaChatModel) {
        boolean isTestProfile = env.acceptsProfiles("test");
        if (isTestProfile) {
            return ChatClient.builder(openAiChatModel);
        } else {
            return ChatClient.builder(ollamaChatModel);
        }
    }
}
