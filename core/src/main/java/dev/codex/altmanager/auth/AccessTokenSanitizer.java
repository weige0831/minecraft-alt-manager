package dev.codex.altmanager.auth;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AccessTokenSanitizer {
    private static final Pattern JWT_PATTERN = Pattern.compile("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

    private AccessTokenSanitizer() {
    }

    public static String extract(String rawValue) {
        if (rawValue == null) {
            return "";
        }
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        Matcher matcher = JWT_PATTERN.matcher(trimmed);
        String firstCandidate = null;
        while (matcher.find()) {
            String candidate = trimJwtCandidate(matcher.group());
            if (firstCandidate == null) {
                firstCandidate = candidate;
            }
            if (AccessTokenIntrospector.inspect(candidate) != null) {
                return candidate;
            }
        }

        if (firstCandidate != null) {
            return firstCandidate;
        }
        if (trimmed.startsWith("tokeneyJ")) {
            return trimmed.substring("token".length());
        }
        return trimmed;
    }

    private static String trimJwtCandidate(String candidate) {
        while (candidate.endsWith(".") || candidate.endsWith(",") || candidate.endsWith(";")) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        int embeddedHeader = candidate.indexOf("eyJ");
        if (embeddedHeader > 0 && embeddedHeader < candidate.indexOf('.')) {
            return candidate.substring(embeddedHeader);
        }
        return candidate;
    }
}
