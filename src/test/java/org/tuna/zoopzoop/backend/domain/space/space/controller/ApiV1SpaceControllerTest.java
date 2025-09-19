package org.tuna.zoopzoop.backend.domain.space.space.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1SpaceControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    // ============================= CREATE ============================= //

    @Test
    @DisplayName("스페이스 생성 - 성공")
    void createSpace_Success() {

    }

    // ========================== HELPER METHODS ========================== //


    /** GET 요청 */
    public ResultActions performGet(String url) throws Exception {
        return mvc.perform(get(url)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    /** POST 요청 */
    public ResultActions performPost(String url, Object body) throws Exception {
        return mvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print());
    }

    /** PUT 요청 */
    public ResultActions performPut(String url, Object body) throws Exception {
        return mvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print());
    }

    /** PATCH 요청 */
    public ResultActions performPatch(String url, Object body) throws Exception {
        return mvc.perform(patch(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print());
    }

    /** DELETE 요청 */
    public ResultActions performDelete(String url, Object body) throws Exception {
        return mvc.perform(delete(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print());
    }

    /** DELETE 요청 (body 없는 경우) */
    public ResultActions performDelete(String url) throws Exception {
        return mvc.perform(delete(url)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }


    // ======================= TEST DATA FACTORIES ======================== //

//    private ClubCreateRequestDto createDefaultClubCreateDto() {
//        return new ClubCreateRequestDto(
//                "테스트 그룹",
//                "테스트 그룹 설명",
//                ClubCategory.TRAVEL,
//                "서울",
//                10,
//                EventType.SHORT_TERM,
//                LocalDate.of(2023, 10, 1),
//                LocalDate.of(2023, 10, 31),
//                true,
//                List.of()
//        );
//    }


    // ====================== COMMON ASSERTIONS ======================= //

    private void expectForbidden(ResultActions resultActions) throws Exception {
        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message").value("권한이 없습니다."));
    }

//    private void expectClubCreated(ResultActions resultActions, Long expectedLeaderId) throws Exception {
//        resultActions.andExpect(status().isCreated())
//                .andExpect(jsonPath("$.code").value(201))
//                .andExpect(jsonPath("$.message").value("클럽이 생성됐습니다."))
//                .andExpect(jsonPath("$.data.clubId").isNumber())
//                .andExpect(jsonPath("$.data.leaderId").value(expectedLeaderId));
//    }
//
//    private void expectClubUpdated(ResultActions resultActions, Long expectedClubId) throws Exception {
//        resultActions.andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(200))
//                .andExpect(jsonPath("$.message").value("클럽 정보가 수정됐습니다."))
//                .andExpect(jsonPath("$.data.clubId").value(expectedClubId));
//    }
//
//    private void expectNotFound(ResultActions resultActions, String message) throws Exception {
//        resultActions.andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.code").value(404))
//                .andExpect(jsonPath("$.message").value(message));
//    }



}