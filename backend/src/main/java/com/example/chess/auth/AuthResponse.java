package com.example.chess.auth;

public record AuthResponse(Long userId, String username, String token) {
}
