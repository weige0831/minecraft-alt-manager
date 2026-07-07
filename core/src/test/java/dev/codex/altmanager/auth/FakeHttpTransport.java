package dev.codex.altmanager.auth;

import dev.codex.altmanager.http.HttpResponse;
import dev.codex.altmanager.http.HttpTransport;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public final class FakeHttpTransport implements HttpTransport {
    private final Queue<HttpResponse> responses = new ArrayDeque<HttpResponse>();
    private final List<Request> requests = new ArrayList<Request>();

    public void enqueue(int statusCode, String body) {
        responses.add(new HttpResponse(statusCode, body, Collections.<String, List<String>>emptyMap()));
    }

    public List<Request> requests() {
        return requests;
    }

    @Override
    public HttpResponse get(String url, Map<String, String> headers) throws IOException {
        requests.add(new Request("GET", url, headers, ""));
        return nextResponse();
    }

    @Override
    public HttpResponse post(String url, Map<String, String> headers, String body) throws IOException {
        requests.add(new Request("POST", url, headers, body));
        return nextResponse();
    }

    private HttpResponse nextResponse() {
        if (responses.isEmpty()) {
            throw new AssertionError("No fake HTTP response queued");
        }
        return responses.remove();
    }

    public static final class Request {
        private final String method;
        private final String url;
        private final Map<String, String> headers;
        private final String body;

        private Request(String method, String url, Map<String, String> headers, String body) {
            this.method = method;
            this.url = url;
            this.headers = headers;
            this.body = body;
        }

        public String method() {
            return method;
        }

        public String url() {
            return url;
        }

        public Map<String, String> headers() {
            return headers;
        }

        public String body() {
            return body;
        }
    }
}
