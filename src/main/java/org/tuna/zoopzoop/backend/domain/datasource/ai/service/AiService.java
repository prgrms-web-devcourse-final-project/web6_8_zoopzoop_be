package org.tuna.zoopzoop.backend.domain.datasource.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.datasource.ai.dto.AiExtractorDto;
import org.tuna.zoopzoop.backend.domain.datasource.ai.dto.AnalyzeContentDto;
import org.tuna.zoopzoop.backend.domain.datasource.ai.prompt.AiPrompt;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.TagRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiService {
    private final ChatClient chatClient;
    private final TagRepository tagRepository;

    public AiExtractorDto extract(String rawHtml) {
        AiExtractorDto response = chatClient.prompt()
                .user(AiPrompt.EXTRACTION.formatted(rawHtml))
                .call()
                .entity(AiExtractorDto.class);

        return response;
    }

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
}