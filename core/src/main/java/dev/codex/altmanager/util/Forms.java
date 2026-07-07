package dev.codex.altmanager.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public final class Forms {
    private Forms() {
    }

    public static String encode(Map<String, String> values) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                builder.append('&');
            }
            first = false;
            builder.append(urlEncode(entry.getKey()));
            builder.append('=');
            builder.append(urlEncode(entry.getValue()));
        }
        return builder.toString();
    }

    public static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 is not available", exception);
        }
    }
}
