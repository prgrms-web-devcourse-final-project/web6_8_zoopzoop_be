package org.tuna.zoopzoop.backend.domain.datasource.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.datasource.ai.dto.AiExtractorDto;
import org.tuna.zoopzoop.backend.domain.datasource.ai.dto.AnalyzeContentDto;
import org.tuna.zoopzoop.backend.domain.datasource.ai.prompt.AiPrompt;
import org.tuna.zoopzoop.backend.domain.datasource.repository.TagRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    public AnalyzeContentDto analyzeContent(String content) {
        // 모든 태그 가져오기
        List<String> allTags = tagRepository.findAllTagNames();

        // 중복 제거 (Set → 다시 List or String)
        Set<String> uniqueTags = new HashSet<>(allTags);

        // JSON 배열 문자열로 변환
        String tags = uniqueTags.stream()
                .map(tag -> "\"" + tag + "\"")   // "tagName"
                .collect(Collectors.joining(", ", "[", "]")); // ["tag1", "tag2"]

        AnalyzeContentDto response = chatClient.prompt()
                .user(AiPrompt.SUMMARY_TAG_CATEGORY.formatted(content, tags))
                .call()
                .entity(AnalyzeContentDto.class);

        return response;
    }
}