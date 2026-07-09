package dev.codex.altmanager.auth;

import dev.codex.altmanager.LoginException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicrosoftAuthServiceTest {
    @Test
    void startDeviceCodeLoginPostsExpectedForm() throws Exception {
        FakeHttpTransport transport = new FakeHttpTransport();
        transport.enqueue(200, "{"
                + "\"device_code\":\"device-123\","
                + "\"user_code\":\"ABCD-EFGH\","
                + "\"verification_uri\":\"https://www.microsoft.com/link\","
                + "\"verification_uri_complete\":\"https://www.microsoft.com/link?otc=ABCD-EFGH\","
                + "\"expires_in\":900,"
                + "\"interval\":5,"
                + "\"message\":\"Use code ABCD-EFGH\""
                + "}");

        MicrosoftAuthConfig config = MicrosoftAuthConfig.builder("client-id").build();
        MicrosoftAuthService service = new MicrosoftAuthService(
                config,
                transport,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                seconds -> {
                }
        );

        DeviceCodeSession session = service.startDeviceCodeLogin();

        assertEquals("device-123", session.getDeviceCode());
        assertEquals("ABCD-EFGH", session.getUserCode());
        assertEquals(Instant.parse("2026-01-01T00:15:00Z"), session.getExpiresAt());
        assertEquals("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode", transport.requests().get(0).url());
        assertEquals("application/x-www-form-urlencoded", transport.requests().get(0).headers().get("Content-Type"));
        assertTrue(transport.requests().get(0).body().contains("client_id=client-id"));
        assertTrue(transport.requests().get(0).body().contains("scope=XboxLive.signin+offline_access"));
    }

    @Test
    void deviceCodeLoginExplainsMobilePublicClientRequirement() {
        FakeHttpTransport transport = new FakeHttpTransport();
        transport.enqueue(401, "{"
                + "\"error\":\"invalid_client\","
                + "\"error_description\":\"AADSTS70002: The provided client is not supported for this feature. "
                + "The client application must be marked as 'mobile.'\""
                + "}");
        MicrosoftAuthService service = new MicrosoftAuthService(
                MicrosoftAuthConfig.builder("client-id").build(),
                transport,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                seconds -> {
                }
        );

        LoginException exception = assertThrows(LoginException.class, service::startDeviceCodeLogin);

        assertTrue(exception.getMessage().contains("HTTP 401"));
        assertTrue(exception.getMessage().contains("AADSTS70002"));
        assertTrue(exception.getMessage().contains("Allow public client flows"));
    }

    @Test
    void deviceCodeLoginExplainsClientConfigurationWhenUnauthorizedBodyIsEmpty() {
        FakeHttpTransport transport = new FakeHttpTransport();
        transport.enqueue(401, "");
        MicrosoftAuthService service = new MicrosoftAuthService(
                MicrosoftAuthConfig.builder("client-id").build(),
                transport,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                seconds -> {
                }
        );

        LoginException exception = assertThrows(LoginException.class, service::startDeviceCodeLogin);

        assertTrue(exception.getMessage().contains("HTTP 401"));
        assertTrue(exception.getMessage().contains("Allow public client flows"));
    }
}
