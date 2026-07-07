package dev.codex.altmanager.store;

import dev.codex.altmanager.AccountKind;
import dev.codex.altmanager.MinecraftAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonAccountStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsAccounts() throws Exception {
        JsonAccountStore store = new JsonAccountStore(tempDir.resolve("accounts.json"));
        MinecraftAccount account = MinecraftAccount.builder()
                .username("Alex")
                .uuid("ec561538f3fd461daff5086b22154bce")
                .accessToken("access")
                .refreshToken("refresh")
                .xuid("xuid")
                .clientId("client")
                .accessTokenExpiresAt(Instant.parse("2026-01-01T00:00:00Z"))
                .kind(AccountKind.MICROSOFT)
                .build();

        store.save(Collections.singletonList(account));
        List<MinecraftAccount> loaded = store.load();

        assertEquals(1, loaded.size());
        assertEquals(account.getUsername(), loaded.get(0).getUsername());
        assertEquals(account.getUuid(), loaded.get(0).getUuid());
        assertEquals(account.getRefreshToken(), loaded.get(0).getRefreshToken());
        assertEquals(account.getAccessTokenExpiresAt(), loaded.get(0).getAccessTokenExpiresAt());
    }

    @Test
    void corruptedStoreLoadsAsEmptyList() throws Exception {
        java.nio.file.Path path = tempDir.resolve("broken.json");
        java.nio.file.Files.write(path, "{not-json".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        JsonAccountStore store = new JsonAccountStore(path);

        assertEquals(0, store.load().size());
    }
}
