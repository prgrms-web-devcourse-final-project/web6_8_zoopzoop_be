package org.tuna.zoopzoop.backend.domain.datasource.service;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.tuna.zoopzoop.backend.domain.datasource.dataprocessor.service.DataProcessorService;
import org.tuna.zoopzoop.backend.domain.datasource.exception.ServiceException;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class DataProcessorServiceTest {

    private DataProcessorService dataProcessorService;

    @BeforeEach
    void setUp() {
        // 실제 객체 생성, 의존성은 테스트에 필요 없으면 null
        dataProcessorService = new DataProcessorService(null, null);
    }

    @Test
    @DisplayName("성공 -> https로 주었을때")
    void checkUrlTest1() {
        // given
        String inputUrl = "https://example.com";

        // when
        Map<String, Object> outputMap = dataProcessorService.checkUrl(inputUrl);
        Document doc = (Document) outputMap.get("document");

        // then
        assertThat(doc).isNotNull();
    }

    @Test
    @DisplayName("성공 -> http로 주었을때")
    void checkUrlTest2() {
        // given
        String inputUrl = "http://example.com";

        // when
        Map<String, Object> outputMap = dataProcessorService.checkUrl(inputUrl);
        Document doc = (Document) outputMap.get("document");

        // then
        assertThat(doc).isNotNull();
    }

    @Test
    @DisplayName("성공 -> 프로토콜 없이 주었을때")
    void checkUrlTest3() {
        // given
        String inputUrl = "example.com";

        // when
        Map<String, Object> outputMap = dataProcessorService.checkUrl(inputUrl);
        Document doc = (Document) outputMap.get("document");

        // then
        assertThat(doc).isNotNull();
    }

    @Test
    @DisplayName("실패 -> 없는 도메인")
    void checkUrlTest4() {
        // given
        String url = "https://this-is-definitely-invalid-url-12345xyz.com";

        // when & then
        assertThatThrownBy(() -> dataProcessorService.checkUrl(url))
                .isInstanceOf(ServiceException.class)
                .hasMessage("URL 접속에 실패했습니다.");
    }

    @Test
    @DisplayName("실패 -> 잘못된 url")
    void checkUrlTest5() {
        // given
        String url = "hts://example.com";

        // when & then
        assertThatThrownBy(() -> dataProcessorService.checkUrl(url))
                .isInstanceOf(ServiceException.class)
                .hasMessage("URL 접속에 실패했습니다.");
    }

    @Test
    @DisplayName("실패 -> 타임아웃 발생 가능한 URL")
    void checkUrlTest6() {
        // given
        String url = "http://example.com:81/"; // 무한로딩

        // when & then
        assertThatThrownBy(() -> dataProcessorService.checkUrl(url))
                .isInstanceOf(ServiceException.class)
                .hasMessage("URL 접속에 실패했습니다.");
    }

    @Test
    @DisplayName("성공 -> https 실패 시 http로 재시도")
    void checkUrlTest7() {
        // given
        String inputUrl = "http://example.com";

        // when
        Map<String, Object> outputMap = dataProcessorService.checkUrl(inputUrl);
        Document doc = (Document) outputMap.get("document");

        // then
        assertThat(doc).isNotNull();
    }
}
