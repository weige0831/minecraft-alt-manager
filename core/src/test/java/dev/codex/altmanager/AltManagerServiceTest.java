package dev.codex.altmanager;

import dev.codex.altmanager.auth.FakeHttpTransport;
import dev.codex.altmanager.session.SessionSwitchResult;
import dev.codex.altmanager.session.SessionSwitchStatus;
import dev.codex.altmanager.auth.AccessTokenIntrospectorTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AltManagerServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void accessTokenLoginSavesAccountThroughFacade() throws Exception {
        FakeHttpTransport transport = new FakeHttpTransport();
        transport.enqueue(200, "{\"items\":[{\"name\":\"game_minecraft\"}]}");
        transport.enqueue(200, "{\"id\":\"069a79f444e94726a5befca90e38aaf5\",\"name\":\"Notch\"}");

        AltManagerService service = AltManagerService.createAccessTokenOnly(tempDir.resolve("accounts.json"), transport);
        MinecraftAccount account = service.loginAccessToken("minecraft-token");
        List<MinecraftAccount> accounts = service.listAccounts();

        assertEquals("Notch", account.getUsername());
        assertEquals(1, accounts.size());
        assertEquals("Notch", accounts.get(0).getUsername());
    }

    @Test
    void microsoftLoginFailsClearlyWhenClientIdMissing() throws Exception {
        AltManagerService service = AltManagerService.createAccessTokenOnly(tempDir.resolve("accounts.json"));

        try {
            service.loginMicrosoft(session -> {
            });
        } catch (LoginException exception) {
            assertEquals("Microsoft client id is not configured", exception.getMessage());
        }
    }

    @Test
    void switchToReportsExpiredTokenBeforeInjecting() {
        MinecraftAccount account = MinecraftAccount.builder()
                .username("Steve")
                .uuid("8667ba71b85a4004af54457a9734eed7")
                .accessToken("token")
                .accessTokenExpiresAt(java.time.Instant.EPOCH)
                .kind(AccountKind.ACCESS_TOKEN)
                .build();
        AltManagerService service = AltManagerService.createAccessTokenOnly(tempDir.resolve("accounts.json"));

        SessionSwitchResult result = service.switchTo(new Object(), account);

        assertEquals(SessionSwitchStatus.TOKEN_EXPIRED, result.getStatus());
    }

    @Test
    void switchToParsesJwtExpiryWhenStoredExpiryIsMissing() {
        MinecraftAccount account = MinecraftAccount.builder()
                .username("LaiShen")
                .uuid("b59ad5d7-bf50-48b0-8dff-8ddd4bc7538d")
                .accessToken(AccessTokenIntrospectorTest.jwt("{"
                        + "\"nbf\":1783252334,"
                        + "\"exp\":1783338734,"
                        + "\"pfd\":[{\"type\":\"mc\",\"id\":\"b59ad5d7-bf50-48b0-8dff-8ddd4bc7538d\",\"name\":\"LaiShen\"}]"
                        + "}"))
                .kind(AccountKind.ACCESS_TOKEN)
                .build();
        AltManagerService service = AltManagerService.createAccessTokenOnly(tempDir.resolve("accounts.json"));

        SessionSwitchResult result = service.switchTo(new Object(), account);

        assertEquals(SessionSwitchStatus.TOKEN_EXPIRED, result.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(result.getMessage().contains("2026-07-06T11:52:14Z"));
    }
}
