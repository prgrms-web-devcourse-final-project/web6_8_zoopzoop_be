package org.tuna.zoopzoop.backend.domain.datasource.ai.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.datasource.ai.dto.AiExtractorDto;
import org.tuna.zoopzoop.backend.domain.datasource.ai.dto.AnalyzeContentDto;
import org.tuna.zoopzoop.backend.domain.datasource.ai.prompt.AiPrompt;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiService {
    private final ChatClient chatClient;

    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 500),
        retryFor = {JsonParseException.class, JsonProcessingException.class}
    )
    public AiExtractorDto extract(String rawHtml) {
        AiExtractorDto response = chatClient.prompt()
                .user(AiPrompt.EXTRACTION.formatted(rawHtml))
                .call()
                .entity(AiExtractorDto.class);

        return response;
    }

    @Recover
    public AiExtractorDto extractRecover(Exception e, String rawHtml) {
        return new AiExtractorDto(
                "",
                null,
                "",
                "",
                ""
        );
    }

    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 500),
        retryFor = {JsonParseException.class, JsonProcessingException.class}
    )
    public AnalyzeContentDto analyzeContent(String content, List<Tag> tagList) {
        // JSON 배열 문자열로 변환
        String tags = tagList.stream()
                .map(Tag::getTagName)   // 태그명만 추출
                .map(tagName -> "\"" + tagName + "\"")
                .collect(Collectors.joining(", ", "[", "]"));

        AnalyzeContentDto response = chatClient.prompt()
                .user(AiPrompt.SUMMARY_TAG_CATEGORY.formatted(content, tags))
                .call()
                .entity(AnalyzeContentDto.class);

        return response;
    }

    @Recover
    public AnalyzeContentDto analyzeContentRecover(Exception e, String content, List<Tag> tagList) {
        return new AnalyzeContentDto(
                "",
                null,
                new ArrayList<>()
        );
    }

}