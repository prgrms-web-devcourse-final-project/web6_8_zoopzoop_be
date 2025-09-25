package org.tuna.zoopzoop.backend.domain.datasource.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.datasource.ai.dto.AiExtractorDto;
import org.tuna.zoopzoop.backend.domain.datasource.ai.prompt.AiPrompt;

@Service
@RequiredArgsConstructor
public class AiService {
    private final ChatClient chatClient;

    public AiExtractorDto extract(String rawHtml) {
        AiExtractorDto response = chatClient.prompt()
                .user(AiPrompt.EXTRACTION.formatted(rawHtml))
                .call()
                .entity(AiExtractorDto.class);

        return response;
    }
}