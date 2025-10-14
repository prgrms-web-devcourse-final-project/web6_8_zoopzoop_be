package org.tuna.zoopzoop.backend.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberSearchRepository;
import org.tuna.zoopzoop.backend.domain.member.service.MemberSearchService;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MemberSearchControllerTest {
    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberSearchService memberSearchService;

    @Autowired
    private MemberSearchRepository memberSearchRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    void setUp() {
        Member member1 = memberService.createMember(
                "검색테스트조건1",
                "url",
                "9123",
                Provider.KAKAO
        );
        Member member2 = memberService.createMember(
                "검색테스트조건2",
                "url",
                "9234",
                Provider.GOOGLE
        );
        Member member3 = memberService.createMember(
                "검색테스트조건3",
                "url",
                "9345",
                Provider.GOOGLE
        );
    }

    @Test
    @WithUserDetails(value = "KAKAO:9123", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("사용자 이름 검색 테스트 - 성공(200)")
    void searchMemberSuccess() throws Exception {
        String keyword1 = "조건1";
        String keyword2 = "조건2";
        String keyword3 = "조건3";
        String keyword4 = "틀림";
        mockMvc.perform(get("/api/v1/member/search")
                        .param("keyword", keyword1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("검색 조건에 맞는 사용자들을 조회 했습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name", containsString(keyword1)));

        mockMvc.perform(get("/api/v1/member/search")
                        .param("keyword", keyword2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("검색 조건에 맞는 사용자들을 조회 했습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name", containsString(keyword2)));

        mockMvc.perform(get("/api/v1/member/search")
                        .param("keyword", keyword3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("검색 조건에 맞는 사용자들을 조회 했습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name", containsString(keyword3)));
        mockMvc.perform(get("/api/v1/member/search")
                        .param("keyword", keyword4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("검색 조건에 맞는 사용자들을 조회 했습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
