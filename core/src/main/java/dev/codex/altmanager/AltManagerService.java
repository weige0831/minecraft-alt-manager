package dev.codex.altmanager;

import dev.codex.altmanager.auth.AccessTokenLoginService;
import dev.codex.altmanager.auth.DeviceCodePrompt;
import dev.codex.altmanager.auth.MicrosoftAuthConfig;
import dev.codex.altmanager.auth.MicrosoftAuthService;
import dev.codex.altmanager.http.HttpTransport;
import dev.codex.altmanager.http.JdkHttpTransport;
import dev.codex.altmanager.session.ClientSessionAdapter;
import dev.codex.altmanager.session.ReflectionClientSessionAdapter;
import dev.codex.altmanager.session.SessionSwitchResult;
import dev.codex.altmanager.session.SessionSwitchService;
import dev.codex.altmanager.store.AccountStore;
import dev.codex.altmanager.store.CredentialStore;
import dev.codex.altmanager.store.InlineCredentialStore;
import dev.codex.altmanager.store.JsonAccountStore;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

public final class AltManagerService {
    private final AccountStore accountStore;
    private final CredentialStore credentialStore;
    private final MicrosoftAuthService microsoftAuthService;
    private final AccessTokenLoginService accessTokenLoginService;
    private final SessionSwitchService sessionSwitchService;

    public AltManagerService(
            AccountStore accountStore,
            CredentialStore credentialStore,
            MicrosoftAuthService microsoftAuthService,
            AccessTokenLoginService accessTokenLoginService,
            SessionSwitchService sessionSwitchService
    ) {
        if (accountStore == null) {
            throw new IllegalArgumentException("accountStore must not be null");
        }
        if (credentialStore == null) {
            throw new IllegalArgumentException("credentialStore must not be null");
        }
        if (accessTokenLoginService == null) {
            throw new IllegalArgumentException("accessTokenLoginService must not be null");
        }
        if (sessionSwitchService == null) {
            throw new IllegalArgumentException("sessionSwitchService must not be null");
        }
        this.accountStore = accountStore;
        this.credentialStore = credentialStore;
        this.microsoftAuthService = microsoftAuthService;
        this.accessTokenLoginService = accessTokenLoginService;
        this.sessionSwitchService = sessionSwitchService;
    }

    public static AltManagerService createDefault(Path accountsPath, String microsoftClientId) {
        return createDefault(accountsPath, microsoftClientId, new JdkHttpTransport());
    }

    public static AltManagerService createDefault(Path accountsPath, String microsoftClientId, HttpTransport transport) {
        JsonAccountStore accountStore = new JsonAccountStore(accountsPath);
        MicrosoftAuthConfig microsoftConfig = MicrosoftAuthConfig.builder(microsoftClientId).build();
        return new AltManagerService(
                accountStore,
                new InlineCredentialStore(),
                new MicrosoftAuthService(microsoftConfig, transport),
                new AccessTokenLoginService(transport, true),
                new SessionSwitchService(new ReflectionClientSessionAdapter(), Clock.systemUTC())
        );
    }

    public static AltManagerService createAccessTokenOnly(Path accountsPath) {
        return createAccessTokenOnly(accountsPath, new JdkHttpTransport());
    }

    public static AltManagerService createAccessTokenOnly(Path accountsPath, HttpTransport transport) {
        JsonAccountStore accountStore = new JsonAccountStore(accountsPath);
        return new AltManagerService(
                accountStore,
                new InlineCredentialStore(),
                null,
                new AccessTokenLoginService(transport, true),
                new SessionSwitchService(new ReflectionClientSessionAdapter(), Clock.systemUTC())
        );
    }

    public List<MinecraftAccount> listAccounts() throws IOException {
        return accountStore.load();
    }

    public MinecraftAccount loginMicrosoft(DeviceCodePrompt prompt) throws IOException, LoginException {
        if (microsoftAuthService == null) {
            throw new LoginException("Microsoft client id is not configured");
        }
        MinecraftAccount account = microsoftAuthService.loginWithDeviceCode(prompt);
        saveAccount(account);
        return account;
    }

    public MinecraftAccount loginAccessToken(String token) throws IOException, LoginException {
        MinecraftAccount account = accessTokenLoginService.login(token);
        saveAccount(account);
        return account;
    }

    public MinecraftAccount refresh(MinecraftAccount account) throws IOException, LoginException {
        if (microsoftAuthService == null) {
            throw new LoginException("Microsoft client id is not configured");
        }
        MinecraftAccount withCredentials = credentialStore.loadCredentials(account);
        MinecraftAccount refreshed = microsoftAuthService.refresh(withCredentials);
        saveAccount(refreshed);
        return refreshed;
    }

    public boolean remove(String uuid) throws IOException {
        credentialStore.removeCredentials(uuid);
        return accountStore.remove(uuid);
    }

    public SessionSwitchResult switchTo(Object minecraftClient, MinecraftAccount account) {
        return sessionSwitchService.switchTo(minecraftClient, account);
    }

    public SessionSwitchResult switchTo(Object minecraftClient, MinecraftAccount account, ClientSessionAdapter adapter) {
        return sessionSwitchService.switchTo(minecraftClient, account, adapter);
    }

    private void saveAccount(MinecraftAccount account) throws IOException {
        MinecraftAccount stored = credentialStore.saveCredentials(account);
        accountStore.addOrReplace(stored);
    }
}
