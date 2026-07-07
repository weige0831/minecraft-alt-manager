package dev.codex.altmanager.forge189;

import dev.codex.altmanager.MinecraftAccount;
import dev.codex.altmanager.session.ClientSessionAdapter;
import dev.codex.altmanager.session.InjectionResult;
import dev.codex.altmanager.session.MinecraftSessionInjector;
import dev.codex.altmanager.session.SessionSwitchResult;
import dev.codex.altmanager.session.SessionSwitchStatus;
import net.minecraft.client.Minecraft;

public final class Forge189SessionAdapter implements ClientSessionAdapter {
    private final MinecraftSessionInjector injector = new MinecraftSessionInjector();

    @Override
    public SessionSwitchResult switchTo(Object minecraftClient, MinecraftAccount account) {
        if (!(minecraftClient instanceof Minecraft)) {
            return SessionSwitchResult.failure(SessionSwitchStatus.FAILED, "Expected Minecraft for Forge 1.8.9");
        }
        Minecraft client = (Minecraft) minecraftClient;
        if (client.theWorld != null || client.getNetHandler() != null) {
            return SessionSwitchResult.failure(
                    SessionSwitchStatus.REQUIRES_DISCONNECT,
                    "Disconnect before switching accounts"
            );
        }
        InjectionResult result = injector.inject(client, account);
        if (result.isSuccess()) {
            return SessionSwitchResult.success("Switched to " + account.getUsername());
        }
        return SessionSwitchResult.failure(SessionSwitchStatus.SESSION_NOT_FOUND, result.getMessage(), result.getCause());
    }
}
