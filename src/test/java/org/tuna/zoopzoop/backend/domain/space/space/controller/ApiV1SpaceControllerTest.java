package org.tuna.zoopzoop.backend.domain.space.space.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.testSupport.ControllerTestSupport;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1SpaceControllerTest extends ControllerTestSupport {

    // ============================= CREATE ============================= //

    @Test
    @DisplayName("스페이스 생성 - 성공")
    void createSpace_Success() throws Exception {
        // Given
        String url = "/api/v1/space";
        String requestBody = createDefaultSpaceCreateRequestBody();

        // When
        ResultActions resultActions = performPost(url, requestBody);

        // Then
        expectCreated(
                resultActions,
                String.format("%s - 스페이스가 생성됐습니다.", "테스트 스페이스")
        );
        resultActions
                .andExpect(jsonPath("$.data.name").value("테스트 스페이스"));
    }

    @Test
    @DisplayName("스페이스 생성 - 실패 : 스페이스명 누락")
    void createSpace_Fail_NameMissing() throws Exception {
        // Given
        String url = "/api/v1/space";
        String requestBody = """
                {
                    "name": ""
                }
                """;

        // When
        ResultActions resultActions = performPost(url, requestBody);

        // Then
        expectBadRequest(
                resultActions,
                "name-NotBlank-must not be blank"
        );
    }

    @Test
    @DisplayName("스페이스 생성 - 실패 : 스페이스명 길이 초과")
    void createSpace_Fail_NameTooLong() throws Exception {
        // Given
        String url = "/api/v1/space";
        String requestBody = """
                {
                    "name": "테스트 스페이스 이름이 너무 길어서 50자를 초과하는 경우입니다. 테스트 스페이스 이름이 너무 길어서 50자를 초과하는 경우입니다."
                }
                """;

        // When
        ResultActions resultActions = performPost(url, requestBody);

        // Then
        expectBadRequest(
                resultActions,
                "name-Length-length must be between 0 and 50"
        );
    }

    @Test
    @DisplayName("스페이스 생성 - 실패 : 스페이스명 중복")
    void createSpace_Fail_NameDuplicate() throws Exception {
        // Given
        String url = "/api/v1/space";
        String requestBody = createDefaultSpaceCreateRequestBody();
        performPost(url, requestBody); // 최초 생성

        // When
        ResultActions resultActions = performPost(url, requestBody); // 중복 생성 시도

        // Then
        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409"))
                .andExpect(jsonPath("$.msg").value("이미 존재하는 스페이스 이름입니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }


    // ======================= TEST DATA FACTORIES ======================== //

    private String createDefaultSpaceCreateRequestBody() {
        return """
                {
                    "name": "테스트 스페이스"
                }
                """;
    }


}