package dev.codex.altmanager.session;

public final class InjectionResult {
    private final boolean success;
    private final String message;
    private final Throwable cause;
    private final String sessionClassName;
    private final String fieldName;

    private InjectionResult(boolean success, String message, Throwable cause, String sessionClassName, String fieldName) {
        this.success = success;
        this.message = message;
        this.cause = cause;
        this.sessionClassName = sessionClassName;
        this.fieldName = fieldName;
    }

    public static InjectionResult success(String message, String sessionClassName, String fieldName) {
        return new InjectionResult(true, message, null, sessionClassName, fieldName);
    }

    public static InjectionResult failure(String message) {
        return new InjectionResult(false, message, null, null, null);
    }

    public static InjectionResult failure(String message, Throwable cause) {
        return new InjectionResult(false, message, cause, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }

    public String getSessionClassName() {
        return sessionClassName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
