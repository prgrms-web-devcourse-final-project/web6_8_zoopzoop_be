package org.tuna.zoopzoop.backend.domain.space.membership.enums;

public enum Authority {
    PENDING, //가입 대기
    READ_ONLY, //읽기만 가능
    READ_WRITE, //읽고 쓰기 가능
    ADMIN //READ & WRITE, 관리 권한
}
