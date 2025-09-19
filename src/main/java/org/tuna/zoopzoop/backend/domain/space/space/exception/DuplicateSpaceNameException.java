package org.tuna.zoopzoop.backend.domain.space.space.exception;

/**
 * 스페이스 이름이 중복될 때 발생하는 예외
 */
public class DuplicateSpaceNameException extends RuntimeException {
    public DuplicateSpaceNameException(String message) {
        super(message);
    }
}
