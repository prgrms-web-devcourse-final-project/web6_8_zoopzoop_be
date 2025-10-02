package org.tuna.zoopzoop.backend.domain.datasource.exception;

public class ServiceException extends RuntimeException {
    public ServiceException(String message) {
        super(message);
    }
}
