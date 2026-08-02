package com.example.microservice.exception;

public class InvalidJwtException extends RuntimeException {
    public InvalidJwtException(String message) {
        super(message);
    }
}