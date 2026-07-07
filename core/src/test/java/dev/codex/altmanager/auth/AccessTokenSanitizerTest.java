package dev.codex.altmanager.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccessTokenSanitizerTest {
    @Test
    void extractsJwtFromNaturalLanguagePaste() {
        String token = AccessTokenIntrospectorTest.jwt("{"
                + "\"exp\":1783338734,"
                + "\"pfd\":[{\"type\":\"mc\",\"id\":\"b59ad5d7-bf50-48b0-8dff-8ddd4bc7538d\",\"name\":\"LaiShen\"}]"
                + "}");

        assertEquals(token, AccessTokenSanitizer.extract("这是一个我的世界账户的token" + token + " 你需要测试"));
    }

    @Test
    void removesPlainTokenPrefixBeforeJwt() {
        String token = AccessTokenIntrospectorTest.jwt("{\"exp\":1783338734}");

        assertEquals(token, AccessTokenSanitizer.extract("token" + token));
    }
}
