package org.tuna.zoopzoop.backend.domain.space.space.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.tuna.zoopzoop.backend.global.rsData.RsData;

@RestControllerAdvice(basePackages = "org.tuna.zoopzoop.backend.domain.space") // 👈 중요!
@Order(0) // 구체적인 핸들러이므로 우선순위를 높게 설정
@Slf4j
public class SpaceExceptionHandler {

    // 중복된 스페이스 이름 예외 처리
    @ExceptionHandler(DuplicateSpaceNameException.class)
    public ResponseEntity<RsData<Void>> handleDuplicateSpaceName(DuplicateSpaceNameException e) {
        return new ResponseEntity<>(
                new RsData<>(
                        "409",
                        e.getMessage(),
                        null
                ),
                HttpStatus.CONFLICT
        );
    }

}
