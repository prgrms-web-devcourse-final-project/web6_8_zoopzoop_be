package org.tuna.zoopzoop.backend.global.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.NoResultException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.tuna.zoopzoop.backend.global.rsData.RsData;

import javax.naming.AuthenticationException;
import java.nio.file.AccessDeniedException;

import static org.springframework.http.HttpStatus.*;

@ControllerAdvice
@Slf4j //Logger 선언
public class GlobalExceptionHandler {
    @Autowired
    private ObjectMapper objectMapper;

    @ExceptionHandler(NoResultException.class) // 자료를 찾지 못했을 경우.
    public ResponseEntity<RsData<Void>> handleNoResultException(NoResultException e) {
        return new ResponseEntity<>(
                new RsData<>(
                        "404",
                        e.getMessage()
                ),
                NOT_FOUND
        );
    }

    @ExceptionHandler(IllegalArgumentException.class) // Request Body 입력 값이 부족할 경우
    public ResponseEntity<RsData<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return new ResponseEntity<>(
                new RsData<>(
                        "400",
                        e.getMessage()
                ),
                BAD_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return new ResponseEntity<>(
                new RsData<>(
                        "400",
                        e.getMessage()
                ),
                BAD_REQUEST
        )
    }

    @ExceptionHandler(AuthenticationException.class) // 인증/인가에 실패한 경우.
    public ResponseEntity<RsData<Void>> handleAuthentication(AuthenticationException e) {
        return new ResponseEntity<>(
                new RsData<>(
                        "401",
                        e.getMessage()
                ),
                UNAUTHORIZED
        );
    }

    @ExceptionHandler(AccessDeniedException.class) // 권한이 부족한 경우
    public ResponseEntity<RsData<Void>> handleAccessDenied(AccessDeniedException e) {
        return new ResponseEntity<>(
                new RsData<>(
                        "403",
                        e.getMessage()
                ),
                FORBIDDEN
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RsData<Void>> handleConflict(DataIntegrityViolationException e) {
        return new ResponseEntity<>(
                new RsData<>(
                        "409",
                        e.getMessage()
                ),
                CONFLICT
        );
    }

    @ExceptionHandler(Exception.class) // 내부 서버 에러(= 따로 Exception을 지정하지 않은 경우.)
    public ResponseEntity<RsData<Void>> handleException(Exception e) {
        return new ResponseEntity<>(
                new RsData<>(
                        "500",
                        e.getMessage()
                ),
                INTERNAL_SERVER_ERROR
        );
    }
}