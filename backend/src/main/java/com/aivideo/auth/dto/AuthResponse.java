package com.aivideo.auth.dto;

public record AuthResponse(
        String token,
        String email,
        String role
) {
}
