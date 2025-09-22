package org.tuna.zoopzoop.backend.domain.member.service;

import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        Member member1 = memberService.createMember(
                "test1",
                1001L,
                "url" );
        Member member2 = memberService.createMember(
                "test2",
                1002L,
                "url" );
    }

    private Member createTestMember() {
        return memberService.createMember(
                "test3",
                1003L,
                "url"
        );
    }

    @Test
    @DisplayName("사용자 생성 - 성공")
    void createMemberSuccess() {
        Member member = createTestMember();
        assertNotNull(member.getId());
        assertEquals("test3", member.getName());
    }

//    @Test
//    @DisplayName("사용자 생성 - 이메일 중복으로 인한 실패")
//    void createMemberFailedByEmail() {
//        memberService.createMember("dupName", 2001L,"url");
//        Exception ex = assertThrows(DataIntegrityViolationException.class, () -> {
//            memberService.createMember("otherName", 2002L,"url");
//        });
//        assertTrue(ex.getMessage().contains("이미 사용중인 이메일입니다."));
//    }

    @Test
    @DisplayName("사용자 생성 - 이름 중복으로 인한 실패")
    void createMemberFailedByName() {
        memberService.createMember("dupName", 3001L,"url");
        Exception ex = assertThrows(DataIntegrityViolationException.class, () -> {
            memberService.createMember("dupName", 3002L,"url");
        });
        assertTrue(ex.getMessage().contains("이미 사용중인 이름입니다."));
    }

//    @Test
//    @DisplayName("사용자 이메일 기반 조회 - 성공")
//    void findByEmailSuccess() {
//        Member saved = createTestMember();
//        Member found = memberService.findByEmail("test3@test.com");
//        assertEquals(saved.getId(), found.getId());
//        assertEquals(saved.getEmail(), found.getEmail());
//    }
//
//    @Test
//    @DisplayName("사용자 이메일 기반 조회 - 실패")
//    void findByEmailFailed() {
//        Exception ex = assertThrows(NoResultException.class, () -> {
//            memberService.findByEmail("wrong@test.com");
//        });
//        assertTrue(ex.getMessage().contains("이메일을 가진 사용자를 찾을 수 없습니다."));
//    }

    @Test
    @DisplayName("사용자 이름 기반 조회 - 성공")
    void findByNameSuccess() {
        Member saved = createTestMember();
        Member found = memberService.findByName("test3");
        assertEquals(saved.getId(), found.getId());
        assertEquals(saved.getName(), found.getName());
    }

    @Test
    @DisplayName("사용자 이름 기반 조회 - 실패")
    void findByNameFailed() {
        Exception ex = assertThrows(NoResultException.class, () -> {
            memberService.findByName("wrongName");
        });
        assertTrue(ex.getMessage().contains("이름을 가진 사용자를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("Kakao 식별 키 기반 조회 - 성공")
    void findByKakaoKeySuccess() {
        Member saved = createTestMember();
        Member found = memberService.findByKakaoKey(1003L);
        assertEquals(saved.getId(), found.getId());
        assertEquals(saved.getName(), found.getName());
    }

    @Test
    @DisplayName("Kakao 식별 키 기반 조회 - 실패")
    void findByKakaoKeyFailed() {
        Exception ex = assertThrows(NoResultException.class, () -> {
            memberService.findByKakaoKey(1004L);
        });
        assertTrue(ex.getMessage().contains("카카오 키를 가진 사용자를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("사용자 이름 변경 - 성공")
    void updateMemberNameSuccess() {
        Member member = createTestMember();
        memberService.updateMemberName(member, "새이름");
        Member updated = memberService.findById(member.getId());
        assertEquals("새이름", updated.getName()); // JUnit 기본 검증
    }

    @Test
    @DisplayName("사용자 이름 변경 - 이름 중복으로 인한 실패")
    void updateMemberNameFailed() {
        Member member = createTestMember();
        Exception ex = assertThrows(DataIntegrityViolationException.class, () -> {
            memberService.updateMemberName(member, "test1");
        });
        assertTrue(ex.getMessage().contains("이미 사용중인 이름입니다."));
    }

    @Test
    @DisplayName("사용자 삭제 - soft delete")
    void softDeleteMember() {
        Member saved = createTestMember();
        memberService.softDeleteMember(saved);
        assertFalse(saved.isActive());
    }

    @Test
    @DisplayName("사용자 삭제 - hard delete")
    void hardDeleteMember() {
        Member saved = createTestMember();
        memberService.hardDeleteMember(saved);
        assertFalse(memberRepository.findById(saved.getId()).isPresent());
    }
}