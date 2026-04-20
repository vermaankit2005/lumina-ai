package com.luminaai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
public class LLMClientConfig {

    @Bean
    @Primary
    @Profile("test")
    public ChatClient.Builder testChatClientBuilder(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel);
    }

    @Bean
    @Primary
    @Profile("!test")
    public ChatClient.Builder prodChatClientBuilder(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel);
    }
}
