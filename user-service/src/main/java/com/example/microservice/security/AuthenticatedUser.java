package com.example.microservice.security;

public record AuthenticatedUser(Long userId, String role) {
}