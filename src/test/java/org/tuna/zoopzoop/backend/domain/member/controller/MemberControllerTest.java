package org.tuna.zoopzoop.backend.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.TagRepository;
import org.tuna.zoopzoop.backend.domain.member.dto.req.ReqBodyForEditMemberName;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;

import static org.hamcrest.Matchers.containsString;
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
    @Autowired
    private DataSourceRepository dataSourceRepository;
    @Qualifier("tagRepository")
    @Autowired
    private TagRepository tagRepository;

    @BeforeAll
    void setUp() {
        tagRepository.deleteAll();
        dataSourceRepository.deleteAll();
        memberRepository.deleteAll();
        Member member1 = memberService.createMember(
                "test1",
                "url",
                "1111",
                Provider.KAKAO
        );
        Member member2 = memberService.createMember(
                "test2",
                "url",
                "2222",
                Provider.GOOGLE
        );
        Member member3 = memberService.createMember(
                "test3",
                "url",
                "3333",
                Provider.GOOGLE
        );
    }

    @AfterAll
    void cleanUp() {
        tagRepository.deleteAll();
        dataSourceRepository.deleteAll();
        memberRepository.deleteAll(); // Graph만 삭제
        // 필요하면 다른 Repository도 순서대로 삭제
    }

    @Test
    @WithUserDetails(value = "KAKAO:1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("사용자 정보 조회 - 성공(200)")
    void getMemberInfoSuccess() throws Exception {
        Member member = memberService.findByProviderKey("1111");
        mockMvc.perform(get("/api/v1/member/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value(member.getName()))
                .andExpect(jsonPath("$.data.profileUrl").value(member.getProfileImageUrl()));
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
    @WithUserDetails(value = "KAKAO:1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("모든 사용자 정보 조회 - 성공(200)")
    void getMemberInfoAllSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/member/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200")) // status는 문자열
                .andExpect(jsonPath("$.msg").value("모든 사용자 정보를 조회했습니다."))
                .andExpect(jsonPath("$.data[0].profileUrl").value("url"));
    }

    @Test
    @WithUserDetails(value = "KAKAO:1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("id 기반 사용자 정보 조회 - 성공(200)")
    void getMemberInfoByIdSuccess() throws Exception {
        Member member = memberService.findByProviderKey("1111");
        int testId = member.getId();
        mockMvc.perform(get("/api/v1/member/{id}", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value(member.getName()))
                .andExpect(jsonPath("$.data.profileUrl").value(member.getProfileImageUrl()));
    }

    @Test
    @WithUserDetails(value = "KAKAO:1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("id 기반 사용자 정보 조회 - 실패(404, Not_Found)")
    void getMemberInfoByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/member/{id}", 10001))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("id 기반 사용자 정보 조회 - 실패(401, Unauthorized)")
    void getMemberInfoByIdUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/member/{id}", 10001))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "KAKAO:1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("이름 기반 사용자 정보 조회 - 성공(200)")
    void getMemberInfoByNameSuccess() throws Exception {
        Member memberByKey = memberService.findByProviderKey("1111");
        Member memberByName = memberService.findByName(memberByKey.getName());
        mockMvc.perform(get("/api/v1/member?name={name}", memberByName.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value(memberByName.getName()))
                .andExpect(jsonPath("$.data.profileUrl").value(memberByName.getProfileImageUrl()));
    }

    @Test
    @WithUserDetails(value = "KAKAO:1111", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("이름 기반 사용자 정보 조회 - 실패(404, Not_Found)")
    void getMemberInfoByNameNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/member?name={name}", "failedName"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("이름 기반 사용자 정보 조회 - 실패(401, Unauthorized)")
    void getMemberInfoByNameUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/member?name={name}", "failedName"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "GOOGLE:2222", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("사용자 이름 수정 - 성공(200)")
    void editMemberNameSuccess() throws Exception {
        ReqBodyForEditMemberName reqBodyForEditMemberName = new ReqBodyForEditMemberName("test3");
        mockMvc.perform(put("/api/v1/member/edit/name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqBodyForEditMemberName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("사용자의 닉네임을 변경했습니다."))
                .andExpect(jsonPath("$.data.name").value(containsString("test")));
    }

    @Test
    @WithUserDetails(value = "GOOGLE:2222", setupBefore = TestExecutionEvent.TEST_METHOD)
    @DisplayName("사용자 이름 수정 - 실패(400, Bad_Request)")
    void editMemberNameFailedByBadRequest() throws Exception {
        ReqBodyForEditMemberName reqBodyForEditMemberName = new ReqBodyForEditMemberName("");
        mockMvc.perform(put("/api/v1/member/edit/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBodyForEditMemberName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").value("newName-NotBlank-잘못된 요청입니다."));
    }

    @Test
    @DisplayName("사용자 이름 수정 - 실패(401, Unauthorized)")
    void editMemberNameFailedByUnauthorized() throws Exception {
        ReqBodyForEditMemberName reqBodyForEditMemberName = new ReqBodyForEditMemberName("test3");
        mockMvc.perform(put("/api/v1/member/edit/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBodyForEditMemberName)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.msg").value("액세스가 거부되었습니다."));
    }

    @Test
    @WithUserDetails(value = "GOOGLE:3333", setupBefore = TestExecutionEvent.TEST_METHOD)
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
