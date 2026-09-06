package com.grangloria.inventory.exception;

import org.springframework.http.HttpStatus;

public abstract class InventoryException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    protected InventoryException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    protected InventoryException(String message, Throwable cause, String errorCode, HttpStatus status) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}