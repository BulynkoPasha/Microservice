package com.example.authservice.exception;

public class InternalAccessDeniedException extends RuntimeException {
    public InternalAccessDeniedException(String message) {
        super(message);
    }
}
