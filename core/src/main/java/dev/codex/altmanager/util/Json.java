package dev.codex.altmanager.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class Json {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private Json() {
    }

    public static JsonObject parseObject(String body) {
        JsonElement element = new JsonParser().parse(body == null || body.isEmpty() ? "{}" : body);
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("Expected JSON object");
        }
        return element.getAsJsonObject();
    }

    public static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    public static long longValue(JsonObject object, String key, long fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsLong();
    }
}
