package org.tuna.zoopzoop.backend.domain.datasource.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.context.ActiveProfiles;
import org.tuna.zoopzoop.backend.domain.datasource.ai.dto.AnalyzeContentDto;
import org.tuna.zoopzoop.backend.domain.datasource.ai.service.AiService;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AiServiceTest {
    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;


    @InjectMocks
    private AiService aiService;

    @Test
    void analyzeContent_ShouldReturnMockedResponse() {
        // given
        String content = "테스트 본문";
        List<Tag> tagList = List.of(new Tag("Java"), new Tag("Spring"));

        AnalyzeContentDto mockResponse = new AnalyzeContentDto(
                "요약",
                Category.IT,
                List.of("Java", "Spring")
        );

        // 체인 mock 세팅
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.entity(AnalyzeContentDto.class)).thenReturn(mockResponse);

        // when
        AnalyzeContentDto result = aiService.analyzeContent(content, tagList);

        // then
        assertThat(result).isNotNull();
        assertThat(result.summary()).isEqualTo("요약");
        assertThat(result.tags()).containsExactly("Java", "Spring");
        assertThat(result.category()).isEqualTo(Category.IT);
    }
}