package dev.codex.altmanager.auth;

import java.time.Instant;

public final class AccessTokenInfo {
    private final Instant notBefore;
    private final Instant expiresAt;
    private final String profileId;
    private final String profileName;

    AccessTokenInfo(Instant notBefore, Instant expiresAt, String profileId, String profileName) {
        this.notBefore = notBefore;
        this.expiresAt = expiresAt;
        this.profileId = profileId;
        this.profileName = profileName;
    }

    public Instant getNotBefore() {
        return notBefore;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getProfileName() {
        return profileName;
    }

    public boolean hasExpiry() {
        return expiresAt != null;
    }
}
