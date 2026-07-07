package dev.codex.altmanager.auth;

import java.time.Instant;

final class TokenSet {
    private final String accessToken;
    private final String refreshToken;
    private final Instant expiresAt;

    TokenSet(String accessToken, String refreshToken, Instant expiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }

    String getAccessToken() {
        return accessToken;
    }

    String getRefreshToken() {
        return refreshToken;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }
}
