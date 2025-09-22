package org.tuna.zoopzoop.backend.global.security;

/**
 * Spring Securiity 구현 전 임시 헬퍼 클래스
 * 추후 Spring Security 연동시 SecurityContext에서 불러오도록 수정
 */

public final class StubAuthUtil {
    private StubAuthUtil() {}

    public static Integer currentMemberId() {
        return 1;
    }

}
