package dev.codex.altmanager.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.codex.altmanager.AccountKind;
import dev.codex.altmanager.LoginException;
import dev.codex.altmanager.MinecraftAccount;
import dev.codex.altmanager.MinecraftProfile;
import dev.codex.altmanager.http.HttpResponse;
import dev.codex.altmanager.http.HttpTransport;
import dev.codex.altmanager.util.Json;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class MinecraftServicesClient {
    private static final String LOGIN_WITH_XBOX_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";
    private static final String ENTITLEMENTS_URL = "https://api.minecraftservices.com/entitlements/mcstore";

    private final HttpTransport transport;
    private final Clock clock;

    MinecraftServicesClient(HttpTransport transport, Clock clock) {
        this.transport = transport;
        this.clock = clock;
    }

    MinecraftAccount loginWithXboxToken(
            XboxToken xstsToken,
            String microsoftRefreshToken,
            String clientId,
            boolean verifyEntitlements
    ) throws LoginException {
        Map<String, String> headers = jsonHeaders();
        JsonObject body = new JsonObject();
        body.addProperty("identityToken", "XBL3.0 x=" + xstsToken.getUhs() + ";" + xstsToken.getToken());

        HttpResponse response = post(LOGIN_WITH_XBOX_URL, headers, Json.GSON.toJson(body));
        JsonObject json = parseSuccessful(response, "Minecraft Services login failed");
        String accessToken = required(json, "access_token", "Minecraft Services response did not contain access_token");
        long expiresIn = Json.longValue(json, "expires_in", 86400);

        return accountFromAccessToken(
                accessToken,
                AccountKind.MICROSOFT,
                microsoftRefreshToken,
                xstsToken.getUhs(),
                clientId,
                clock.instant().plusSeconds(expiresIn),
                verifyEntitlements
        );
    }

    MinecraftAccount accountFromAccessToken(
            String accessToken,
            AccountKind kind,
            String refreshToken,
            String xuid,
            String clientId,
            Instant expiresAt,
            boolean verifyEntitlements
    ) throws LoginException {
        if (verifyEntitlements && !ownsMinecraft(accessToken)) {
            throw new LoginException("This account does not appear to own Minecraft Java Edition");
        }
        MinecraftProfile profile = fetchProfile(accessToken);
        return MinecraftAccount.builder()
                .username(profile.getName())
                .uuid(profile.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .xuid(xuid)
                .clientId(clientId)
                .accessTokenExpiresAt(expiresAt)
                .kind(kind)
                .build();
    }

    MinecraftProfile fetchProfile(String accessToken) throws LoginException {
        HttpResponse response = get(PROFILE_URL, bearerHeaders(accessToken));
        JsonObject json = parseSuccessful(response, "Minecraft profile request failed");
        String id = required(json, "id", "Minecraft profile response did not contain id");
        String name = required(json, "name", "Minecraft profile response did not contain name");
        return new MinecraftProfile(id, name);
    }

    boolean ownsMinecraft(String accessToken) throws LoginException {
        HttpResponse response = get(ENTITLEMENTS_URL, bearerHeaders(accessToken));
        JsonObject json = parseSuccessful(response, "Minecraft entitlements request failed");
        JsonArray items = json.has("items") && json.get("items").isJsonArray()
                ? json.getAsJsonArray("items")
                : new JsonArray();
        for (JsonElement item : items) {
            if (!item.isJsonObject()) {
                continue;
            }
            String name = Json.string(item.getAsJsonObject(), "name");
            if ("game_minecraft".equals(name) || "product_minecraft".equals(name)) {
                return true;
            }
        }
        return false;
    }

    private HttpResponse get(String url, Map<String, String> headers) throws LoginException {
        try {
            return transport.get(url, headers);
        } catch (IOException exception) {
            throw new LoginException("HTTP GET failed for " + url, exception);
        }
    }

    private HttpResponse post(String url, Map<String, String> headers, String body) throws LoginException {
        try {
            return transport.post(url, headers, body);
        } catch (IOException exception) {
            throw new LoginException("HTTP POST failed for " + url, exception);
        }
    }

    private JsonObject parseSuccessful(HttpResponse response, String message) throws LoginException {
        JsonObject json;
        try {
            json = Json.parseObject(response.getBody());
        } catch (RuntimeException exception) {
            throw new LoginException(message + ": invalid JSON response, HTTP " + response.getStatusCode(), exception);
        }
        if (!response.isSuccessful()) {
            String error = Json.string(json, "error");
            String errorMessage = Json.string(json, "errorMessage");
            String detail = error != null ? error : errorMessage;
            throw new LoginException(message + ": HTTP " + response.getStatusCode() + (detail == null ? "" : " (" + detail + ")"));
        }
        return json;
    }

    private String required(JsonObject json, String key, String message) throws LoginException {
        String value = Json.string(json, key);
        if (value == null || value.trim().isEmpty()) {
            throw new LoginException(message);
        }
        return value;
    }

    private Map<String, String> jsonHeaders() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        return headers;
    }

    private Map<String, String> bearerHeaders(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);
        headers.put("Accept", "application/json");
        return headers;
    }
}
