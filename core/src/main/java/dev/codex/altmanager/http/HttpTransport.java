package dev.codex.altmanager.http;

import java.io.IOException;
import java.util.Map;

public interface HttpTransport {
    HttpResponse get(String url, Map<String, String> headers) throws IOException;

    HttpResponse post(String url, Map<String, String> headers, String body) throws IOException;
}
