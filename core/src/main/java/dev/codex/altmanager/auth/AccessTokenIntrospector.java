package dev.codex.altmanager.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.codex.altmanager.util.Json;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public final class AccessTokenIntrospector {
    private AccessTokenIntrospector() {
    }

    public static AccessTokenInfo inspect(String token) {
        if (token == null) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(pad(parts[1])), StandardCharsets.UTF_8);
            JsonObject payload = Json.parseObject(payloadJson);
            Instant notBefore = instant(payload, "nbf");
            Instant expiresAt = instant(payload, "exp");
            ProfileClaim profile = profile(payload);
            return new AccessTokenInfo(
                    notBefore,
                    expiresAt,
                    profile == null ? null : profile.id,
                    profile == null ? null : profile.name
            );
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static Instant instant(JsonObject payload, String key) {
        if (!payload.has(key) || payload.get(key).isJsonNull()) {
            return null;
        }
        return Instant.ofEpochSecond(payload.get(key).getAsLong());
    }

    private static ProfileClaim profile(JsonObject payload) {
        if (payload.has("pfd") && payload.get("pfd").isJsonArray()) {
            JsonArray profiles = payload.getAsJsonArray("pfd");
            if (profiles.size() > 0 && profiles.get(0).isJsonObject()) {
                JsonObject profile = profiles.get(0).getAsJsonObject();
                return new ProfileClaim(Json.string(profile, "id"), Json.string(profile, "name"));
            }
        }
        if (payload.has("profiles") && payload.get("profiles").isJsonObject()) {
            JsonObject profiles = payload.getAsJsonObject("profiles");
            String minecraftProfile = Json.string(profiles, "mc");
            if (minecraftProfile != null) {
                return new ProfileClaim(minecraftProfile, null);
            }
        }
        return null;
    }

    private static byte[] pad(String value) {
        int remainder = value.length() % 4;
        if (remainder == 2) {
            value += "==";
        } else if (remainder == 3) {
            value += "=";
        } else if (remainder == 1) {
            value += "===";
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static final class ProfileClaim {
        private final String id;
        private final String name;

        private ProfileClaim(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
