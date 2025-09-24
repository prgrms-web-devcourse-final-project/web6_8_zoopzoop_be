package org.tuna.zoopzoop.backend.domain.datasource.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.datasource.repository.TagRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiService {
    private final ChatClient chatClient;
    private final TagRepository tagRepository;

    public Set<String> duplicateTag() {
        Set<String> existingTags = new HashSet<>();
        existingTags.addAll(tagRepository.findAllTagNames());
        return existingTags;
    }

    public Map<String, Object> summarizeAndTag(String text) {
        Set<String> existingTags = duplicateTag();
        String tagsForPrompt = String.join(", ", existingTags);

        String prompt = """
            본문 요약 프롬프트:
            아래 본문을 무조건 50자 이상, 100자 이하로 요약해주세요.
            
            핵심 태그 프롬프트:
            이미 존재하는 태그 목록은 다음과 같습니다:
            [%s]
            
            본문을 요약하고, 해당 본문과 관련된 태그 3~5개를 생성하세요.
            - 태그는 반드시 본문과 관련된 것만 선택하세요.
            - 기존 태그 중 본문과 관련 없는 것은 포함하지 마세요.
            - 새로운 태그는 본문에 꼭 필요한 경우에만 생성하세요.
            - 결과는 JSON 형식으로만 출력하세요.
            
            본문:
            %s
            
            예시 출력:
            {
              "summary": "...",
              "tags": ["...", "..."]
            }
            """.formatted(tagsForPrompt, text);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // JSON 시작/끝만 추출
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}") + 1;
        if (start >= 0 && end > start) {
            response = response.substring(start, end);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = mapper.readValue(response, new TypeReference<Map<String, Object>>() {});

            String summary = (String) map.get("summary");
            List<String> tags = (List<String>) map.get("tags");

            return map;
        } catch (Exception e) {
            throw new RuntimeException("AI 응답 파싱 실패: " + response, e);
        }
    }
}