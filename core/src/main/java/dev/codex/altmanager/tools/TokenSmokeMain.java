package dev.codex.altmanager.tools;

import dev.codex.altmanager.LoginException;
import dev.codex.altmanager.MinecraftAccount;
import dev.codex.altmanager.auth.AccessTokenInfo;
import dev.codex.altmanager.auth.AccessTokenIntrospector;
import dev.codex.altmanager.auth.AccessTokenLoginService;
import dev.codex.altmanager.auth.AccessTokenSanitizer;

public final class TokenSmokeMain {
    private TokenSmokeMain() {
    }

    public static void main(String[] args) {
        String token = args.length > 0 ? args[0] : System.getenv("ALT_MANAGER_TOKEN");
        if (token == null || token.trim().isEmpty()) {
            System.err.println("Set ALT_MANAGER_TOKEN or pass a Minecraft Services access token as the first argument.");
            System.exit(64);
            return;
        }

        token = AccessTokenSanitizer.extract(token);
        AccessTokenInfo info = AccessTokenIntrospector.inspect(token);
        if (info != null) {
            System.out.println("token-claim name=" + nullToUnknown(info.getProfileName())
                    + " id=" + nullToUnknown(info.getProfileId())
                    + " notBefore=" + nullToUnknown(info.getNotBefore())
                    + " expiresAt=" + nullToUnknown(info.getExpiresAt()));
        }

        try {
            MinecraftAccount account = AccessTokenLoginService.createDefault().login(token);
            System.out.println("LOGIN_OK name=" + account.getUsername()
                    + " uuid=" + account.getUuid()
                    + " kind=" + account.getKind()
                    + " expiresAt=" + nullToUnknown(account.getAccessTokenExpiresAt()));
        } catch (LoginException exception) {
            System.err.println("LOGIN_FAILED " + exception.getMessage());
            System.exit(2);
        }
    }

    private static String nullToUnknown(Object value) {
        return value == null ? "unknown" : String.valueOf(value);
    }
}
