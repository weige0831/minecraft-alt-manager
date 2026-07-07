package dev.codex.altmanager.auth;

import dev.codex.altmanager.AccountKind;
import dev.codex.altmanager.LoginException;
import dev.codex.altmanager.MinecraftAccount;
import dev.codex.altmanager.http.HttpTransport;
import dev.codex.altmanager.http.JdkHttpTransport;

import java.time.Clock;
import java.time.Instant;

public final class AccessTokenLoginService {
    private final MinecraftServicesClient minecraftServices;
    private final boolean verifyEntitlements;
    private final Clock clock;

    public AccessTokenLoginService(HttpTransport transport, boolean verifyEntitlements) {
        this(transport, Clock.systemUTC(), verifyEntitlements);
    }

    AccessTokenLoginService(HttpTransport transport, Clock clock, boolean verifyEntitlements) {
        this.minecraftServices = new MinecraftServicesClient(transport, clock);
        this.verifyEntitlements = verifyEntitlements;
        this.clock = clock;
    }

    public static AccessTokenLoginService createDefault() {
        return new AccessTokenLoginService(new JdkHttpTransport(), true);
    }

    public static AccessTokenLoginService createDefault(boolean verifyEntitlements) {
        return new AccessTokenLoginService(new JdkHttpTransport(), verifyEntitlements);
    }

    public MinecraftAccount login(String minecraftServicesAccessToken) throws LoginException {
        minecraftServicesAccessToken = AccessTokenSanitizer.extract(minecraftServicesAccessToken);
        AccessTokenInfo info = AccessTokenIntrospector.inspect(minecraftServicesAccessToken);
        validateTokenWindow(info);
        return login(minecraftServicesAccessToken, info == null ? null : info.getExpiresAt());
    }

    public MinecraftAccount login(String minecraftServicesAccessToken, Instant expiresAt) throws LoginException {
        minecraftServicesAccessToken = AccessTokenSanitizer.extract(minecraftServicesAccessToken);
        if (minecraftServicesAccessToken == null || minecraftServicesAccessToken.trim().isEmpty()) {
            throw new LoginException("Minecraft Services access token must not be blank");
        }
        if (expiresAt != null && !expiresAt.isAfter(clock.instant())) {
            throw new LoginException("Minecraft Services access token expired at " + expiresAt);
        }
        return minecraftServices.accountFromAccessToken(
                minecraftServicesAccessToken,
                AccountKind.ACCESS_TOKEN,
                null,
                null,
                null,
                expiresAt,
                verifyEntitlements
        );
    }

    private void validateTokenWindow(AccessTokenInfo info) throws LoginException {
        if (info == null) {
            return;
        }
        Instant now = clock.instant();
        if (info.getNotBefore() != null && info.getNotBefore().isAfter(now)) {
            throw new LoginException("Minecraft Services access token is not valid before " + info.getNotBefore());
        }
        if (info.getExpiresAt() != null && !info.getExpiresAt().isAfter(now)) {
            throw new LoginException("Minecraft Services access token expired at " + info.getExpiresAt());
        }
    }
}
