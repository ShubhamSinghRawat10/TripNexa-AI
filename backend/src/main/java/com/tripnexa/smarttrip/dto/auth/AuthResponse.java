package com.tripnexa.smarttrip.dto.auth;

import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    UserSummary user
) {
    public record UserSummary(UUID id, String name, String email) {}
}
