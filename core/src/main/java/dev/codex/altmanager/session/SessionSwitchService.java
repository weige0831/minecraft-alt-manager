package dev.codex.altmanager.session;

import dev.codex.altmanager.MinecraftAccount;
import dev.codex.altmanager.auth.AccessTokenInfo;
import dev.codex.altmanager.auth.AccessTokenIntrospector;

import java.time.Clock;
import java.time.Instant;

public final class SessionSwitchService {
    private final ClientSessionAdapter defaultAdapter;
    private final Clock clock;

    public SessionSwitchService(ClientSessionAdapter defaultAdapter, Clock clock) {
        if (defaultAdapter == null) {
            throw new IllegalArgumentException("defaultAdapter must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.defaultAdapter = defaultAdapter;
        this.clock = clock;
    }

    public SessionSwitchResult switchTo(Object minecraftClient, MinecraftAccount account) {
        return switchTo(minecraftClient, account, defaultAdapter);
    }

    public SessionSwitchResult switchTo(Object minecraftClient, MinecraftAccount account, ClientSessionAdapter adapter) {
        if (minecraftClient == null) {
            return SessionSwitchResult.failure(SessionSwitchStatus.FAILED, "Minecraft client instance is missing");
        }
        if (account == null) {
            return SessionSwitchResult.failure(SessionSwitchStatus.FAILED, "Minecraft account is missing");
        }
        Instant now = clock.instant();
        if (account.isAccessTokenExpired(now)) {
            return SessionSwitchResult.failure(SessionSwitchStatus.TOKEN_EXPIRED, "Minecraft access token expired at " + account.getAccessTokenExpiresAt());
        }
        AccessTokenInfo info = AccessTokenIntrospector.inspect(account.getAccessToken());
        if (info != null && info.getExpiresAt() != null && !info.getExpiresAt().isAfter(now)) {
            return SessionSwitchResult.failure(SessionSwitchStatus.TOKEN_EXPIRED, "Minecraft access token expired at " + info.getExpiresAt());
        }
        if (info != null && info.getNotBefore() != null && info.getNotBefore().isAfter(now)) {
            return SessionSwitchResult.failure(SessionSwitchStatus.TOKEN_EXPIRED, "Minecraft access token is not valid before " + info.getNotBefore());
        }
        return adapter.switchTo(minecraftClient, account);
    }
}
