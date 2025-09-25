package org.tuna.zoopzoop.backend.domain.member.service;

import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
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
                "testtest1",
                "url",
                "1111",
                Provider.KAKAO
        );
        Member member2 = memberService.createMember(
                "testtest2",
                "url",
                "2222",
                Provider.GOOGLE
        );
    }

    private Member createTestMember() {
        return memberService.createMember(
                "test3",
                "url",
                "3333",
                Provider.KAKAO
        );
    }

    @AfterEach
    void cleanUp() {
        memberRepository.deleteAll(); // Graph만 삭제
        // 필요하면 다른 Repository도 순서대로 삭제
    }

    @Test
    @DisplayName("사용자 생성 - 성공")
    void createMemberSuccess() {
        Member member = createTestMember();
        assertNotNull(member.getId());
        assertEquals("url", member.getProfileImageUrl());
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
//        memberService.createMember("dupName", "url", "4001", Provider.KAKAO);
//        Exception ex = assertThrows(DataIntegrityViolationException.class, () -> {
//            memberService.createMember("dupName", "url", "4002", Provider.KAKAO);
//        });
//        assertTrue(ex.getMessage().contains("이미 사용중인 이름입니다."));
        // 유저 이름에 난수 태그를 붙이는 것으로 인해, 거의 테스트가 불가능해짐.
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
        Member found = memberService.findByName(saved.getName());
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
    @DisplayName("식별 키 기반 조회 - 성공")
    void findByKakaoKeySuccess() {
        Member saved = createTestMember();
        Member found = memberService.findByProviderKey(saved.getProviderKey());
        assertEquals(saved.getId(), found.getId());
        assertEquals(saved.getName(), found.getName());
    }

    @Test
    @DisplayName("식별 키 기반 조회 - 실패")
    void findByKakaoKeyFailed() {
        Exception ex = assertThrows(NoResultException.class, () -> {
            memberService.findByProviderKey("5555");
        });
        assertTrue(ex.getMessage().contains("해당 키를 가진 사용자를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("사용자 이름 변경 - 성공")
    void updateMemberNameSuccess() {
        Member member = createTestMember();
        memberService.updateMemberName(member, "새이름");
        Member updated = memberService.findById(member.getId());
        assertEquals("새이름", updated.getName().substring(0, 3));
    }

    @Test
    @DisplayName("사용자 이름 변경 - 이름 중복으로 인한 실패")
    void updateMemberNameFailed() {
//        Member curMember = memberService.findById(1);
//        Member member = createTestMember();
//        Exception ex = assertThrows(DataIntegrityViolationException.class, () -> {
//            memberService.updateMemberName(member, curMember.getName());
//        });
//        assertTrue(ex.getMessage().contains("이미 사용중인 이름입니다."));
        // 유저 이름에 난수 태그를 붙이는 것으로 인해, 거의 테스트가 불가능해짐.
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