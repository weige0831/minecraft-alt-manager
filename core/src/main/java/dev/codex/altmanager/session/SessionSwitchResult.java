package dev.codex.altmanager.session;

public final class SessionSwitchResult {
    private final SessionSwitchStatus status;
    private final String message;
    private final Throwable cause;

    private SessionSwitchResult(SessionSwitchStatus status, String message, Throwable cause) {
        this.status = status;
        this.message = message;
        this.cause = cause;
    }

    public static SessionSwitchResult success(String message) {
        return new SessionSwitchResult(SessionSwitchStatus.SUCCESS, message, null);
    }

    public static SessionSwitchResult failure(SessionSwitchStatus status, String message) {
        return new SessionSwitchResult(status, message, null);
    }

    public static SessionSwitchResult failure(SessionSwitchStatus status, String message, Throwable cause) {
        return new SessionSwitchResult(status, message, cause);
    }

    public SessionSwitchStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }

    public boolean isSuccess() {
        return status == SessionSwitchStatus.SUCCESS;
    }
}
