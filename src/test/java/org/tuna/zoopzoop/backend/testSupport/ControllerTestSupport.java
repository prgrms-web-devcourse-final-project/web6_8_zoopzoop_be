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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    protected ResultActions performGet(String url) throws Exception {
        return mvc.perform(get(url)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    protected ResultActions performPost(String url, Object body) throws Exception {
        return mvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print());
    }

    // ... 다른 perform 메서드들도 모두 이곳으로 이동 ...
    protected ResultActions performPatch(String url, Object body) throws Exception {
        return mvc.perform(patch(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print());
    }

    protected ResultActions performDelete(String url) throws Exception {
        return mvc.perform(delete(url)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    // ====================== COMMON ASSERTIONS (Response) ======================= //

    protected void expectForbidden(ResultActions resultActions) throws Exception {
        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message").value("권한이 없습니다."));
    }

    protected void expectForbidden(ResultActions resultActions, String msg) throws Exception {
        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message").value(msg));
    }
}
