package dev.codex.altmanager.auth;

import java.time.Instant;

final class XboxToken {
    private final String token;
    private final String uhs;
    private final Instant expiresAt;

    XboxToken(String token, String uhs, Instant expiresAt) {
        this.token = token;
        this.uhs = uhs;
        this.expiresAt = expiresAt;
    }

    String getToken() {
        return token;
    }

    String getUhs() {
        return uhs;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }
}
