package org.tuna.zoopzoop.backend.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.tuna.zoopzoop.backend.domain.member.dto.req.ReqBodyForEditMemberName;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MemberControllerTest {
    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    void setUp() {
        memberService.createMember(
                "test",
                4001L,
                "url");
        memberService.createMember(
                "test2",
                4002L,
                "url");
        memberService.createMember(
                "test3",
                4003L,
                "url");
    }

    @Test
    @WithUserDetails(value = "4001", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("사용자 정보 조회 - 성공(200)")
    void getMemberInfoSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/member/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("test"))
                .andExpect(jsonPath("$.data.profileUrl").value("url"));
    }

    @Test
    @DisplayName("사용자 정보 조회 - 실패(401, Unauthorized)")
    void getMemberInfoFailed() throws Exception {
        mockMvc.perform(get("/api/v1/member/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.msg").value("액세스가 거부되었습니다."));
    }

    @Test
    @WithUserDetails(value = "4001", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("사용자 이름 수정 - 성공(200)")
    void editMemberNameSuccess() throws Exception {
        ReqBodyForEditMemberName reqBodyForEditMemberName = new ReqBodyForEditMemberName("test3");
        mockMvc.perform(put("/api/v1/member/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqBodyForEditMemberName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("사용자의 닉네임을 변경했습니다."))
                .andExpect(jsonPath("$.data.name").value("test3"));
    }

    @Test
    @WithUserDetails(value = "4001", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("사용자 이름 수정 - 실패(400, Bad_Request)")
    void editMemberNameFailedByBadRequest() throws Exception {
        ReqBodyForEditMemberName reqBodyForEditMemberName = new ReqBodyForEditMemberName("");
        mockMvc.perform(put("/api/v1/member/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBodyForEditMemberName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("사용자 이름 수정 - 실패(401, Unauthorized)")
    void editMemberNameFailedByUnauthorized() throws Exception {
        ReqBodyForEditMemberName reqBodyForEditMemberName = new ReqBodyForEditMemberName("test3");
        mockMvc.perform(put("/api/v1/member/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBodyForEditMemberName)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.msg").value("액세스가 거부되었습니다."));
    }

    @Test
    @WithUserDetails(value = "4003", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("사용자 삭제 - 성공(200)")
    void deleteMemberSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/member"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("정상적으로 탈퇴되었습니다."));
    }

    @Test
    @DisplayName("사용자 삭제 - 실패(401, Unauthorized)")
    void deleteMemberFailed() throws Exception {
        mockMvc.perform(delete("/api/v1/member"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.msg").value("액세스가 거부되었습니다."));
    }
}
