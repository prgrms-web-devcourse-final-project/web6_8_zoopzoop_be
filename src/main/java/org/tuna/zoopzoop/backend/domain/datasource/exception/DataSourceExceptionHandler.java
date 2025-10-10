package org.tuna.zoopzoop.backend.domain.datasource.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.tuna.zoopzoop.backend.global.rsData.RsData;

@ControllerAdvice
public class DataSourceExceptionHandler {
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<RsData<Void>> ServiceExceptionHandler(ServiceException e) {
        return new ResponseEntity<>(
                new RsData<>(
                        "400",
                        e.getMessage(),
                        null
                ),
                HttpStatus.BAD_REQUEST
        );
    }
}