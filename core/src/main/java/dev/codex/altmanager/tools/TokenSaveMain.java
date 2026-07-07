package dev.codex.altmanager.tools;

import dev.codex.altmanager.AltManagerService;
import dev.codex.altmanager.LoginException;
import dev.codex.altmanager.MinecraftAccount;
import dev.codex.altmanager.auth.AccessTokenSanitizer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class TokenSaveMain {
    private TokenSaveMain() {
    }

    public static void main(String[] args) {
        String token = args.length > 0 ? args[0] : System.getenv("ALT_MANAGER_TOKEN");
        String accountsPath = args.length > 1 ? args[1] : System.getenv("ALT_MANAGER_ACCOUNTS_PATH");
        if (token == null || token.trim().isEmpty()) {
            System.err.println("Set ALT_MANAGER_TOKEN or pass a Minecraft Services access token as the first argument.");
            System.exit(64);
            return;
        }
        if (accountsPath == null || accountsPath.trim().isEmpty()) {
            System.err.println("Set ALT_MANAGER_ACCOUNTS_PATH or pass the accounts JSON path as the second argument.");
            System.exit(64);
            return;
        }

        try {
            Path path = Paths.get(accountsPath);
            MinecraftAccount account = AltManagerService.createAccessTokenOnly(path)
                    .loginAccessToken(AccessTokenSanitizer.extract(token));
            System.out.println("SAVED_OK name=" + account.getUsername()
                    + " uuid=" + account.getUuid()
                    + " expiresAt=" + account.getAccessTokenExpiresAt()
                    + " path=" + path.toAbsolutePath());
        } catch (LoginException exception) {
            System.err.println("SAVE_FAILED " + exception.getMessage());
            System.exit(2);
        } catch (IOException exception) {
            System.err.println("SAVE_FAILED " + exception.getMessage());
            System.exit(74);
        }
    }
}
