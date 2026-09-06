package com.grangloria.returns.exception;

// A custom exception makes the loggers and handlers cleaner
public class ReturnNotFoundException extends RuntimeException {
    public ReturnNotFoundException(String trackingId) {
        super("Return manifest not found for tracking ID: " + trackingId);
    }
}