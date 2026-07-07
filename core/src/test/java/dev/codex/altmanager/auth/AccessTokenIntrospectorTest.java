package dev.codex.altmanager.auth;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AccessTokenIntrospectorTest {
    @Test
    void extractsProfileAndExpiryFromMinecraftJwt() {
        String token = jwt("{"
                + "\"nbf\":1783252334,"
                + "\"exp\":1783338734,"
                + "\"pfd\":[{\"type\":\"mc\",\"id\":\"b59ad5d7-bf50-48b0-8dff-8ddd4bc7538d\",\"name\":\"LaiShen\"}]"
                + "}");

        AccessTokenInfo info = AccessTokenIntrospector.inspect(token);

        assertEquals("LaiShen", info.getProfileName());
        assertEquals("b59ad5d7-bf50-48b0-8dff-8ddd4bc7538d", info.getProfileId());
        assertEquals(Instant.ofEpochSecond(1783252334L), info.getNotBefore());
        assertEquals(Instant.ofEpochSecond(1783338734L), info.getExpiresAt());
    }

    @Test
    void ignoresNonJwtTokens() {
        assertNull(AccessTokenIntrospector.inspect("not-a-jwt"));
    }

    public static String jwt(String payloadJson) {
        return encode("{\"alg\":\"none\"}") + "." + encode(payloadJson) + ".signature";
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
