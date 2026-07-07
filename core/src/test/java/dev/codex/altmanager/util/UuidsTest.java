package dev.codex.altmanager.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UuidsTest {
    @Test
    void normalizesUndashedMinecraftProfileIds() {
        assertEquals(
                "069a79f4-44e9-4726-a5be-fca90e38aaf5",
                Uuids.normalize("069a79f444e94726a5befca90e38aaf5")
        );
    }
}
