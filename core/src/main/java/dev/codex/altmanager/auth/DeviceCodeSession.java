package dev.codex.altmanager.auth;

import java.time.Clock;
import java.time.Instant;

public final class DeviceCodeSession {
    private final String deviceCode;
    private final String userCode;
    private final String verificationUri;
    private final String verificationUriComplete;
    private final String message;
    private final int intervalSeconds;
    private final Instant expiresAt;

    public DeviceCodeSession(
            String deviceCode,
            String userCode,
            String verificationUri,
            String verificationUriComplete,
            String message,
            int intervalSeconds,
            Instant expiresAt
    ) {
        this.deviceCode = requireNonBlank(deviceCode, "deviceCode");
        this.userCode = requireNonBlank(userCode, "userCode");
        this.verificationUri = requireNonBlank(verificationUri, "verificationUri");
        this.verificationUriComplete = verificationUriComplete;
        this.message = message;
        this.intervalSeconds = intervalSeconds <= 0 ? 5 : intervalSeconds;
        this.expiresAt = expiresAt;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public String getUserCode() {
        return userCode;
    }

    public String getVerificationUri() {
        return verificationUri;
    }

    public String getVerificationUriComplete() {
        return verificationUriComplete;
    }

    public String getMessage() {
        return message;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(Clock clock) {
        return expiresAt != null && !expiresAt.isAfter(clock.instant());
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
