package com.example.microservice.exception;

public class CardLimitExceededException extends RuntimeException {

    public CardLimitExceededException(String message) {
        super(message);
    }
}
