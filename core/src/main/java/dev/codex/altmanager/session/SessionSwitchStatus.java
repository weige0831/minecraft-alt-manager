package dev.codex.altmanager.session;

public enum SessionSwitchStatus {
    SUCCESS,
    SESSION_NOT_FOUND,
    REQUIRES_DISCONNECT,
    PROFILE_KEY_REFRESH_FAILED,
    TOKEN_EXPIRED,
    FAILED
}
