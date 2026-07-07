package dev.codex.altmanager.util;

import java.util.Locale;
import java.util.UUID;

public final class Uuids {
    private Uuids() {
    }

    public static String normalize(String value) {
        String stripped = stripDashes(value);
        if (stripped.length() != 32) {
            return UUID.fromString(value).toString();
        }
        return (stripped.substring(0, 8) + '-' +
                stripped.substring(8, 12) + '-' +
                stripped.substring(12, 16) + '-' +
                stripped.substring(16, 20) + '-' +
                stripped.substring(20)).toLowerCase(Locale.ROOT);
    }

    public static String stripDashes(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("-", "").toLowerCase(Locale.ROOT);
    }
}
