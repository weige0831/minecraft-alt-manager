package dev.codex.altmanager.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class JdkHttpTransport implements HttpTransport {
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    public JdkHttpTransport() {
        this(15000, 30000);
    }

    public JdkHttpTransport(int connectTimeoutMillis, int readTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public HttpResponse get(String url, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = open(url, "GET", headers);
        return read(connection);
    }

    @Override
    public HttpResponse post(String url, Map<String, String> headers, String body) throws IOException {
        HttpURLConnection connection = open(url, "POST", headers);
        connection.setDoOutput(true);
        byte[] bytes = (body == null ? "" : body).getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);
        OutputStream output = connection.getOutputStream();
        try {
            output.write(bytes);
        } finally {
            output.close();
        }
        return read(connection);
    }

    private HttpURLConnection open(String url, String method, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(connectTimeoutMillis);
        connection.setReadTimeout(readTimeoutMillis);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "minecraft-alt-manager/0.1");
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }
        return connection;
    }

    private HttpResponse read(HttpURLConnection connection) throws IOException {
        int statusCode = connection.getResponseCode();
        InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String body = readBody(stream);
        try {
            return new HttpResponse(statusCode, body, connection.getHeaderFields());
        } finally {
            connection.disconnect();
        }
    }

    private String readBody(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } finally {
            reader.close();
        }
    }
}
