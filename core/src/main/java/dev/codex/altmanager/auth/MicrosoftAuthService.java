package dev.codex.altmanager.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.codex.altmanager.LoginException;
import dev.codex.altmanager.MinecraftAccount;
import dev.codex.altmanager.http.HttpResponse;
import dev.codex.altmanager.http.HttpTransport;
import dev.codex.altmanager.http.JdkHttpTransport;
import dev.codex.altmanager.util.Forms;
import dev.codex.altmanager.util.Json;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MicrosoftAuthService {
    private static final String DEVICE_CODE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";
    private static final String XBOX_USER_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";

    private final MicrosoftAuthConfig config;
    private final HttpTransport transport;
    private final MinecraftServicesClient minecraftServices;
    private final Clock clock;
    private final Sleeper sleeper;

    public MicrosoftAuthService(MicrosoftAuthConfig config, HttpTransport transport) {
        this(config, transport, Clock.systemUTC(), new ThreadSleeper());
    }

    MicrosoftAuthService(MicrosoftAuthConfig config, HttpTransport transport, Clock clock, Sleeper sleeper) {
        this.config = config;
        this.transport = transport;
        this.clock = clock;
        this.sleeper = sleeper;
        this.minecraftServices = new MinecraftServicesClient(transport, clock);
    }

    public static MicrosoftAuthService createDefault(MicrosoftAuthConfig config) {
        return new MicrosoftAuthService(config, new JdkHttpTransport());
    }

    public DeviceCodeSession startDeviceCodeLogin() throws LoginException {
        Map<String, String> form = new LinkedHashMap<String, String>();
        form.put("client_id", config.getClientId());
        form.put("scope", config.getScopeString());

        HttpResponse response = postForm(config.getDeviceCodeUrl(), form);
        JsonObject json = parseSuccessful(response, "Microsoft device code request failed");

        String verificationUri = firstPresent(json, "verification_uri", "verification_url");
        long expiresIn = Json.longValue(json, "expires_in", 900);
        long interval = Json.longValue(json, "interval", 5);
        return new DeviceCodeSession(
                required(json, "device_code", "Microsoft response did not contain device_code"),
                required(json, "user_code", "Microsoft response did not contain user_code"),
                requiredValue(verificationUri, "Microsoft response did not contain verification_uri"),
                Json.string(json, "verification_uri_complete"),
                Json.string(json, "message"),
                (int) interval,
                clock.instant().plusSeconds(expiresIn)
        );
    }

    public MinecraftAccount loginWithDeviceCode(DeviceCodePrompt prompt) throws LoginException {
        DeviceCodeSession session = startDeviceCodeLogin();
        return completeDeviceCodeLogin(session, prompt);
    }

    public MinecraftAccount completeDeviceCodeLogin(DeviceCodeSession session) throws LoginException {
        return completeDeviceCodeLogin(session, null);
    }

    public MinecraftAccount completeDeviceCodeLogin(DeviceCodeSession session, DeviceCodePrompt prompt) throws LoginException {
        if (prompt != null) {
            prompt.show(session);
        }
        TokenSet microsoftTokens = pollDeviceCode(session);
        return loginWithMicrosoftAccessToken(
                microsoftTokens.getAccessToken(),
                microsoftTokens.getRefreshToken(),
                microsoftTokens.getExpiresAt()
        );
    }

    public MinecraftAccount refresh(MinecraftAccount account) throws LoginException {
        if (account == null || !account.hasRefreshToken()) {
            throw new LoginException("Account does not contain a Microsoft refresh token");
        }
        Map<String, String> form = new LinkedHashMap<String, String>();
        form.put("client_id", config.getClientId());
        form.put("scope", config.getScopeString());
        form.put("grant_type", "refresh_token");
        form.put("refresh_token", account.getRefreshToken());

        HttpResponse response = postForm(config.getTokenUrl(), form);
        TokenSet tokens = parseMicrosoftTokenResponse(response, "Microsoft refresh failed");
        return loginWithMicrosoftAccessToken(tokens.getAccessToken(), tokens.getRefreshToken(), tokens.getExpiresAt());
    }

    public MinecraftAccount loginWithMicrosoftAccessToken(String microsoftAccessToken) throws LoginException {
        return loginWithMicrosoftAccessToken(microsoftAccessToken, null, null);
    }

    public MinecraftAccount loginWithMicrosoftAccessToken(
            String microsoftAccessToken,
            String microsoftRefreshToken,
            Instant microsoftExpiresAt
    ) throws LoginException {
        if (microsoftAccessToken == null || microsoftAccessToken.trim().isEmpty()) {
            throw new LoginException("Microsoft access token must not be blank");
        }
        XboxToken xboxToken = authenticateXboxLive(microsoftAccessToken);
        XboxToken xstsToken = authorizeXsts(xboxToken);
        return minecraftServices.loginWithXboxToken(
                xstsToken,
                microsoftRefreshToken,
                config.getClientId(),
                config.isVerifyEntitlements()
        );
    }

    private TokenSet pollDeviceCode(DeviceCodeSession session) throws LoginException {
        int intervalSeconds = session.getIntervalSeconds();
        while (true) {
            if (session.isExpired(clock)) {
                throw new LoginException("Microsoft device code expired before authorization completed");
            }

            Map<String, String> form = new LinkedHashMap<String, String>();
            form.put("client_id", config.getClientId());
            form.put("grant_type", DEVICE_CODE_GRANT);
            form.put("device_code", session.getDeviceCode());

            HttpResponse response = postForm(config.getTokenUrl(), form);
            if (response.isSuccessful()) {
                return parseMicrosoftTokenResponse(response, "Microsoft token request failed");
            }

            JsonObject json = tryParseObject(response.getBody());
            if (json == null) {
                throw new LoginException(describeHttpFailure("Microsoft token request failed", response, null));
            }
            String error = Json.string(json, "error");
            if ("authorization_pending".equals(error)) {
                sleep(intervalSeconds);
                continue;
            }
            if ("slow_down".equals(error)) {
                intervalSeconds += 5;
                sleep(intervalSeconds);
                continue;
            }
            if ("authorization_declined".equals(error)) {
                throw new LoginException("Microsoft device code authorization was declined");
            }
            if ("expired_token".equals(error)) {
                throw new LoginException("Microsoft device code expired");
            }
            throw new LoginException(describeHttpFailure("Microsoft token request failed", response, json));
        }
    }

    private XboxToken authenticateXboxLive(String microsoftAccessToken) throws LoginException {
        JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", "d=" + microsoftAccessToken);

        JsonObject body = new JsonObject();
        body.add("Properties", properties);
        body.addProperty("RelyingParty", "http://auth.xboxlive.com");
        body.addProperty("TokenType", "JWT");

        HttpResponse response = postJson(XBOX_USER_AUTH_URL, body);
        JsonObject json = parseSuccessful(response, "Xbox Live user authentication failed");
        return parseXboxToken(json, "Xbox Live user authentication response");
    }

    private XboxToken authorizeXsts(XboxToken xboxToken) throws LoginException {
        JsonArray userTokens = new JsonArray();
        userTokens.add(xboxToken.getToken());

        JsonObject properties = new JsonObject();
        properties.addProperty("SandboxId", "RETAIL");
        properties.add("UserTokens", userTokens);

        JsonObject body = new JsonObject();
        body.add("Properties", properties);
        body.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        body.addProperty("TokenType", "JWT");

        HttpResponse response = postJson(XSTS_AUTH_URL, body);
        JsonObject json = parseObject(response, "Xbox XSTS authorization failed");
        if (!response.isSuccessful()) {
            throw new LoginException("Xbox XSTS authorization failed: HTTP " + response.getStatusCode()
                    + describeXstsError(json));
        }
        return parseXboxToken(json, "Xbox XSTS authorization response");
    }

    private XboxToken parseXboxToken(JsonObject json, String label) throws LoginException {
        String token = required(json, "Token", label + " did not contain Token");
        String uhs = extractUhs(json);
        String notAfter = Json.string(json, "NotAfter");
        Instant expiresAt = null;
        if (notAfter != null && !notAfter.trim().isEmpty()) {
            try {
                expiresAt = Instant.parse(notAfter);
            } catch (RuntimeException ignored) {
                expiresAt = null;
            }
        }
        return new XboxToken(token, uhs, expiresAt);
    }

    private String extractUhs(JsonObject json) throws LoginException {
        if (!json.has("DisplayClaims") || !json.get("DisplayClaims").isJsonObject()) {
            throw new LoginException("Xbox response did not contain DisplayClaims");
        }
        JsonObject displayClaims = json.getAsJsonObject("DisplayClaims");
        if (!displayClaims.has("xui") || !displayClaims.get("xui").isJsonArray()) {
            throw new LoginException("Xbox response did not contain DisplayClaims.xui");
        }
        JsonArray xui = displayClaims.getAsJsonArray("xui");
        if (xui.size() == 0 || !xui.get(0).isJsonObject()) {
            throw new LoginException("Xbox response did not contain a valid xui claim");
        }
        String uhs = Json.string(xui.get(0).getAsJsonObject(), "uhs");
        return requiredValue(uhs, "Xbox response did not contain user hash");
    }

    private TokenSet parseMicrosoftTokenResponse(HttpResponse response, String message) throws LoginException {
        JsonObject json = parseSuccessful(response, message);
        String accessToken = required(json, "access_token", message + ": response did not contain access_token");
        String refreshToken = Json.string(json, "refresh_token");
        long expiresIn = Json.longValue(json, "expires_in", 3600);
        return new TokenSet(accessToken, refreshToken, clock.instant().plusSeconds(expiresIn));
    }

    private HttpResponse postForm(String url, Map<String, String> form) throws LoginException {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Accept", "application/json");
        return post(url, headers, Forms.encode(form));
    }

    private HttpResponse postJson(String url, JsonObject body) throws LoginException {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        return post(url, headers, Json.GSON.toJson(body));
    }

    private HttpResponse post(String url, Map<String, String> headers, String body) throws LoginException {
        try {
            return transport.post(url, headers, body);
        } catch (IOException exception) {
            throw new LoginException("HTTP POST failed for " + url, exception);
        }
    }

    private JsonObject parseSuccessful(HttpResponse response, String message) throws LoginException {
        if (!response.isSuccessful()) {
            throw new LoginException(describeHttpFailure(message, response, tryParseObject(response.getBody())));
        }
        return parseObject(response, message);
    }

    private JsonObject parseObject(HttpResponse response, String message) throws LoginException {
        try {
            return Json.parseObject(response.getBody());
        } catch (RuntimeException exception) {
            throw new LoginException(message + ": invalid JSON response, HTTP " + response.getStatusCode(), exception);
        }
    }

    private JsonObject tryParseObject(String body) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            return Json.parseObject(body);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String describeHttpFailure(String message, HttpResponse response, JsonObject json) {
        StringBuilder builder = new StringBuilder(message)
                .append(": HTTP ")
                .append(response.getStatusCode());
        String error = json == null ? null : Json.string(json, "error");
        String description = json == null ? null : Json.string(json, "error_description");
        if (error != null && !error.trim().isEmpty()) {
            builder.append(" (").append(error).append(')');
        }
        if (description != null && !description.trim().isEmpty()) {
            builder.append(' ').append(description);
        }
        if (isDeviceCodeClientConfigurationFailure(message, response.getStatusCode(), description)) {
            builder.append(" Hint: this Microsoft client id is not enabled for device code/public client flow. ")
                    .append("In Azure App Registration, add a Mobile and desktop platform and enable ")
                    .append("'Allow public client flows', or configure another public client id.");
        }
        return builder.toString();
    }

    private boolean isDeviceCodeClientConfigurationFailure(String message, int statusCode, String description) {
        if (message == null || !message.contains("device code request")) {
            return false;
        }
        if (description != null && (description.contains("AADSTS70002")
                || description.contains("must be marked as 'mobile'"))) {
            return true;
        }
        return statusCode == 401;
    }

    private String required(JsonObject json, String key, String message) throws LoginException {
        String value = Json.string(json, key);
        return requiredValue(value, message);
    }

    private String requiredValue(String value, String message) throws LoginException {
        if (value == null || value.trim().isEmpty()) {
            throw new LoginException(message);
        }
        return value;
    }

    private String firstPresent(JsonObject json, String first, String second) {
        String value = Json.string(json, first);
        return value == null ? Json.string(json, second) : value;
    }

    private String describeXstsError(JsonObject json) {
        long xerr = Json.longValue(json, "XErr", -1);
        if (xerr == -1) {
            return "";
        }
        String meaning;
        if (xerr == 2148916233L) {
            meaning = "Xbox profile is missing";
        } else if (xerr == 2148916235L) {
            meaning = "Xbox Live is not available in this region";
        } else if (xerr == 2148916236L || xerr == 2148916237L) {
            meaning = "Xbox account requires adult verification";
        } else if (xerr == 2148916238L) {
            meaning = "Xbox child account cannot proceed without family approval";
        } else {
            meaning = "XErr=" + xerr;
        }
        return " (" + meaning + ")";
    }

    private void sleep(int seconds) throws LoginException {
        try {
            sleeper.sleepSeconds(seconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LoginException("Interrupted while waiting for Microsoft device authorization", exception);
        }
    }
}
