package org.tuna.zoopzoop.backend.global.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.NoResultException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.tuna.zoopzoop.backend.global.rsData.RsData;

import javax.naming.AuthenticationException;
import java.nio.file.AccessDeniedException;
import java.util.stream.Collectors;

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

    @ExceptionHandler(MethodArgumentNotValidException.class) // 유효하지 않은 메소드 파라미터 예외
    public ResponseEntity<RsData<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("잘못된 요청입니다."); // 메세지가 없을 경우 기본값

        return new ResponseEntity<>(
                new RsData<>(
                        "400",
                        message
                ),
                BAD_REQUEST
        );
    }

    @ExceptionHandler(ConstraintViolationException.class) // 제약사항 위반 예외
    public ResponseEntity<RsData<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations()
                .stream()
                .map(violation -> {
                    String path = violation.getPropertyPath().toString();
                    String field = path.contains(".") ? path.substring(path.indexOf('.') + 1) : path;
                    String[] bits = violation.getMessageTemplate().split("\\.");
                    String code = bits.length >= 2 ? bits[bits.length - 2] : "Unknown";
                    String msg = violation.getMessage();
                    return field + "-" + code + "-" + msg;
                })
                .sorted()
                .collect(Collectors.joining("\n"));

        return new ResponseEntity<>(
                new RsData<>(
                        "400",
                        e.getMessage()
                ),
                BAD_REQUEST
        );
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

    @ExceptionHandler(DataIntegrityViolationException.class) // 중복된 데이터의 경우
    public ResponseEntity<RsData<Void>> handleConflict(DataIntegrityViolationException e) {
        return new ResponseEntity<>(
                new RsData<>(
                        "409",
                        e.getMessage()
                ),
                CONFLICT
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class) // 파라미터 타입 관련 예외.
    public ResponseEntity<RsData<Void>> handle(MethodArgumentTypeMismatchException ex) {
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "알 수 없음";
        String msg = "파라미터 '" + ex.getName() + "'의 타입이 올바르지 않습니다. 요구되는 타입: " + requiredType;
        return new ResponseEntity<>(
                new RsData<>(
                        "400",
                        msg
                ),
                BAD_REQUEST
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class) // multi-part 관련 예외 처리
    public ResponseEntity<RsData<Void>> handle(MissingServletRequestPartException e) {
        String msg = "필수 multipart 파트 '" + e.getRequestPartName() + "'가 존재하지 않습니다.";
        return new ResponseEntity<>(
                new RsData<>(
                        "400",
                        msg
                ),
                BAD_REQUEST
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