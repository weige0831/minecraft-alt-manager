package dev.codex.altmanager.session;

import dev.codex.altmanager.AccountKind;
import dev.codex.altmanager.MinecraftAccount;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftSessionInjectorTest {
    @Test
    void injectsLegacyFourStringSession() {
        LegacyClient client = new LegacyClient();
        MinecraftAccount account = account();

        InjectionResult result = new MinecraftSessionInjector().inject(client, account);

        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals("Steve", client.session.username);
        assertEquals("8667ba71b85a4004af54457a9734eed7", client.session.uuid);
        assertEquals("token", client.session.accessToken);
        assertEquals("msa", client.session.type);
    }

    @Test
    void injectsModernUuidOptionalEnumSession() {
        ModernClient client = new ModernClient();
        MinecraftAccount account = account();

        InjectionResult result = new MinecraftSessionInjector().inject(client, account);

        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals("Steve", client.session.username);
        assertEquals(UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7"), client.session.uuid);
        assertEquals("token", client.session.accessToken);
        assertEquals(Optional.of("xuid"), client.session.xuid);
        assertEquals(Optional.of("client-id"), client.session.clientId);
        assertEquals(AccountType.MSA, client.session.accountType);
    }

    private MinecraftAccount account() {
        return MinecraftAccount.builder()
                .username("Steve")
                .uuid("8667ba71b85a4004af54457a9734eed7")
                .accessToken("token")
                .refreshToken("refresh")
                .xuid("xuid")
                .clientId("client-id")
                .kind(AccountKind.MICROSOFT)
                .build();
    }

    static final class LegacyClient {
        private LegacySession session;
    }

    static final class LegacySession {
        private final String username;
        private final String uuid;
        private final String accessToken;
        private final String type;

        LegacySession(String username, String uuid, String accessToken, String type) {
            this.username = username;
            this.uuid = uuid;
            this.accessToken = accessToken;
            this.type = type;
        }
    }

    static final class ModernClient {
        private ModernSession session;
    }

    enum AccountType {
        LEGACY,
        MSA
    }

    static final class ModernSession {
        private final String username;
        private final UUID uuid;
        private final String accessToken;
        private final Optional<String> xuid;
        private final Optional<String> clientId;
        private final AccountType accountType;

        ModernSession(
                String username,
                UUID uuid,
                String accessToken,
                Optional<String> xuid,
                Optional<String> clientId,
                AccountType accountType
        ) {
            this.username = username;
            this.uuid = uuid;
            this.accessToken = accessToken;
            this.xuid = xuid;
            this.clientId = clientId;
            this.accountType = accountType;
        }
    }
}
