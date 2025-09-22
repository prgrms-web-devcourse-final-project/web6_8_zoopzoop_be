package org.tuna.zoopzoop.backend.domain.auth.service;

import org.tuna.zoopzoop.backend.domain.member.entity.Member;

import java.util.Map;

public interface OAuth2UserInfoService {
    boolean supports(String registrationId); // 이 서비스가 해당 provider를 처리하는지
    Member processUser(Map<String, Object> attributes);
}