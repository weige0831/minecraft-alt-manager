package dev.codex.altmanager.store;

import dev.codex.altmanager.AccountKind;
import dev.codex.altmanager.MinecraftAccount;
import dev.codex.altmanager.util.Json;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class JsonAccountStore implements AccountStore {
    private final Path path;

    public JsonAccountStore(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public List<MinecraftAccount> load() throws IOException {
        if (!Files.exists(path)) {
            return new ArrayList<MinecraftAccount>();
        }
        BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        try {
            StoredAccounts stored = Json.GSON.fromJson(reader, StoredAccounts.class);
            if (stored == null || stored.accounts == null) {
                return new ArrayList<MinecraftAccount>();
            }
            List<MinecraftAccount> accounts = new ArrayList<MinecraftAccount>();
            for (StoredAccount account : stored.accounts) {
                if (account != null) {
                    accounts.add(account.toAccount());
                }
            }
            return accounts;
        } catch (RuntimeException exception) {
            return new ArrayList<MinecraftAccount>();
        } finally {
            reader.close();
        }
    }

    @Override
    public void save(Collection<MinecraftAccount> accounts) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        StoredAccounts stored = new StoredAccounts();
        stored.accounts = new ArrayList<StoredAccount>();
        if (accounts != null) {
            for (MinecraftAccount account : accounts) {
                stored.accounts.add(StoredAccount.from(account));
            }
        }

        Path temp = Files.createTempFile(parent, path.getFileName().toString(), ".tmp");
        BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8);
        try {
            Json.GSON.toJson(stored, writer);
        } finally {
            writer.close();
        }

        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static final class StoredAccounts {
        private List<StoredAccount> accounts;
    }

    private static final class StoredAccount {
        private String username;
        private String uuid;
        private String accessToken;
        private String refreshToken;
        private String xuid;
        private String clientId;
        private String accessTokenExpiresAt;
        private AccountKind kind;

        private static StoredAccount from(MinecraftAccount account) {
            StoredAccount stored = new StoredAccount();
            stored.username = account.getUsername();
            stored.uuid = account.getUuid();
            stored.accessToken = account.getAccessToken();
            stored.refreshToken = account.getRefreshToken();
            stored.xuid = account.getXuid();
            stored.clientId = account.getClientId();
            stored.accessTokenExpiresAt = account.getAccessTokenExpiresAt() == null
                    ? null
                    : account.getAccessTokenExpiresAt().toString();
            stored.kind = account.getKind();
            return stored;
        }

        private MinecraftAccount toAccount() {
            return MinecraftAccount.builder()
                    .username(username)
                    .uuid(uuid)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .xuid(xuid)
                    .clientId(clientId)
                    .accessTokenExpiresAt(accessTokenExpiresAt == null ? null : Instant.parse(accessTokenExpiresAt))
                    .kind(kind)
                    .build();
        }
    }
}
