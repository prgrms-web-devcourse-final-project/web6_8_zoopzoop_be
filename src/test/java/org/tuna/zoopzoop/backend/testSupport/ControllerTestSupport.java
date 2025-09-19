package org.tuna.zoopzoop.backend.testSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller 계층의 테스트를 위한 추상 클래스
 * MockMvc와 ObjectMapper를 자동으로 주입받아 자식 클래스에서 사용하도록 제공한다.
 * 또한 공통적인 요청 수행 메서드와 응답 검증 메서드를 제공한다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class ControllerTestSupport {
    @Autowired
    protected MockMvc mvc; // 자식 클래스에서 직접 접근할 수도 있도록 protected로 변경

    @Autowired
    protected ObjectMapper objectMapper;

    // ========================== HELPER METHODS (Request) ========================== //

    /**
     * GET 요청을 수행하는 헬퍼 메서드
     * @param url - 요청할 URL
     * @return ResultActions - MockMvc의 ResultActions 객체
     * @throws Exception - 예외 발생 시 던짐
     */
    protected ResultActions performGet(String url) throws Exception {
        return mvc.perform(get(url)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    /**
     * POST 요청을 수행하는 헬퍼 메서드
     * @param url - 요청할 URL
     * @param body - 요청 바디 (객체 형태)
     * @return ResultActions - MockMvc의 ResultActions 객체
     * @throws Exception - 예외 발생 시 던짐
     */
    protected ResultActions performPost(String url, String body) throws Exception {
        return mvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print());
    }

    /**
     * PUT 요청을 수행하는 헬퍼 메서드
     * @param url - 요청할 URL
     * @param body - 요청 바디 (객체 형태)
     * @return ResultActions - MockMvc의 ResultActions 객체
     * @throws Exception - 예외 발생 시 던짐
     */
    protected ResultActions performPatch(String url, String body) throws Exception {
        return mvc.perform(patch(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print());
    }

    /**
     * DELETE 요청을 수행하는 헬퍼 메서드
     * @param url - 요청할 URL
     * @return ResultActions - MockMvc의 ResultActions 객체
     * @throws Exception - 예외 발생 시 던짐
     */
    protected ResultActions performDelete(String url) throws Exception {
        return mvc.perform(delete(url)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    // ====================== COMMON ASSERTIONS (Response) ======================= //

    /**
     * 200 OK 응답을 기대하는 헬퍼 메서드
     * @param resultActions - MockMvc의 ResultActions 객체
     * @param msg - 기대하는 메시지
     * @throws Exception - 예외 발생 시 던짐
     */
    protected void expectOk(ResultActions resultActions, String msg) throws Exception {
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value(msg));
    }

    /**
     * 201 Created 응답을 기대하는 헬퍼 메서드
     * @param resultActions - MockMvc의 ResultActions 객체
     * @param msg - 기대하는 메시지
     * @throws Exception - 예외 발생 시 던짐
     */
    protected void expectCreated(ResultActions resultActions, String msg) throws Exception {
        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201"))
                .andExpect(jsonPath("$.msg").value(msg));
    }

    /**
     * 400 Bad Request 응답을 기대하는 헬퍼 메서드
     * @param resultActions - MockMvc의 ResultActions 객체
     * @param msg - 기대하는 메시지
     * @throws Exception - 예외 발생 시 던짐
     */
    protected void expectBadRequest(ResultActions resultActions, String msg) throws Exception {
        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400"))
                .andExpect(jsonPath("$.msg").value(msg))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    /**
     * 403 Forbidden 응답을 기대하는 헬퍼 메서드
     * @param resultActions - MockMvc의 ResultActions 객체
     * @throws Exception - 예외 발생 시 던짐
     */
    protected void expectForbidden(ResultActions resultActions) throws Exception {
        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403"))
                .andExpect(jsonPath("$.msg").value("권한이 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    /**
     * 403 Forbidden 응답을 기대하는 헬퍼 메서드 (메시지 커스터마이징)
     * @param resultActions - MockMvc의 ResultActions 객체
     * @param msg - 기대하는 메시지
     * @throws Exception - 예외 발생 시 던짐
     */
    protected void expectForbidden(ResultActions resultActions, String msg) throws Exception {
        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403"))
                .andExpect(jsonPath("$.msg").value(msg))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }


}
