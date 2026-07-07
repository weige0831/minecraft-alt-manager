package dev.codex.altmanager.auth;

import dev.codex.altmanager.AccountKind;
import dev.codex.altmanager.LoginException;
import dev.codex.altmanager.MinecraftAccount;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessTokenLoginServiceTest {
    @Test
    void loginUsesEntitlementsAndProfile() throws Exception {
        FakeHttpTransport transport = new FakeHttpTransport();
        transport.enqueue(200, "{\"items\":[{\"name\":\"game_minecraft\"}]}");
        transport.enqueue(200, "{\"id\":\"069a79f444e94726a5befca90e38aaf5\",\"name\":\"Notch\"}");

        AccessTokenLoginService service = new AccessTokenLoginService(transport, true);
        MinecraftAccount account = service.login("minecraft-token");

        assertEquals("Notch", account.getUsername());
        assertEquals("069a79f4-44e9-4726-a5be-fca90e38aaf5", account.getUuid());
        assertEquals("minecraft-token", account.getAccessToken());
        assertEquals(AccountKind.ACCESS_TOKEN, account.getKind());

        assertEquals("GET", transport.requests().get(0).method());
        assertEquals("https://api.minecraftservices.com/entitlements/mcstore", transport.requests().get(0).url());
        assertEquals("Bearer minecraft-token", transport.requests().get(0).headers().get("Authorization"));
        assertEquals("https://api.minecraftservices.com/minecraft/profile", transport.requests().get(1).url());
    }

    @Test
    void expiredJwtFailsBeforeHttpRequests() throws Exception {
        FakeHttpTransport transport = new FakeHttpTransport();
        AccessTokenLoginService service = new AccessTokenLoginService(
                transport,
                Clock.fixed(Instant.parse("2026-07-06T22:51:28Z"), ZoneOffset.UTC),
                true
        );
        String token = AccessTokenIntrospectorTest.jwt("{"
                + "\"nbf\":1783252334,"
                + "\"exp\":1783338734,"
                + "\"pfd\":[{\"type\":\"mc\",\"id\":\"b59ad5d7-bf50-48b0-8dff-8ddd4bc7538d\",\"name\":\"LaiShen\"}]"
                + "}");

        try {
            service.login(token);
            fail("Expected expired token to fail before HTTP");
        } catch (LoginException exception) {
            assertTrue(exception.getMessage().contains("expired at 2026-07-06T11:52:14Z"));
        }

        assertEquals(0, transport.requests().size());
    }
}
