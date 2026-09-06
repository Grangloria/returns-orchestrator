package com.grangloria.inventory.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class InventoryExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(InventoryExceptionHandler.class);

    @ExceptionHandler(InventoryException.class)
    public ResponseEntity<ErrorResponseDto> handleInventoryException(
            InventoryException ex, ServerHttpRequest request) {
        log.warn("Domain exception caught [{}]: {}", ex.getErrorCode(), ex.getMessage());

        ErrorResponseDto response = new ErrorResponseDto(
                ex.getMessage(),
                ex.getStatus().value(),
                Instant.now().toEpochMilli(),
                List.of(String.format("Path: %s | ErrorCode: %s", request.getPath().value(), ex.getErrorCode()))
        );

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(
            WebExchangeBindException ex, ServerHttpRequest request) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
                .toList();

        log.warn("Payload validation failed for path {}: {}", request.getPath().value(), errors);

        ErrorResponseDto response = new ErrorResponseDto(
                "Validation failed for request payload",
                HttpStatus.BAD_REQUEST.value(),
                Instant.now().toEpochMilli(),
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnhandledException(
            Exception ex, ServerHttpRequest request) {
        log.error("Unhandled internal server error occurred on path {}", request.getPath().value(), ex);

        ErrorResponseDto response = new ErrorResponseDto(
                "An unexpected error occurred while processing inventory request",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                Instant.now().toEpochMilli(),
                List.of(String.format("Path: %s", request.getPath().value()))
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}