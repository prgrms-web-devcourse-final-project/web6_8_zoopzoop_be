package org.tuna.zoopzoop.backend.global.webMvc;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AiConfig {

    private final OpenAiChatModel openAiChatModel;

    @Bean
    public ChatClient chatClient() {
        return ChatClient.builder(openAiChatModel).build();
    }
}
