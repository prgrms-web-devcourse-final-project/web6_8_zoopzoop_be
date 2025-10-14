package org.tuna.zoopzoop.backend.domain.auth.service.oauth2;

import org.tuna.zoopzoop.backend.domain.member.entity.Member;

import java.util.Map;

public interface OAuth2UserInfoService {
    boolean supports(String registrationId); // 이 서비스가 해당 provider(Google, Kakao)를 처리하는지
    Member processUser(Map<String, Object> attributes); // 받아온 정보를 바탕으로 Member 엔티티 생성 or 가져오기
}