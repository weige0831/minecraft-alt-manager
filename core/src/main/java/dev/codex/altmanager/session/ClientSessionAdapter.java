package dev.codex.altmanager.session;

import dev.codex.altmanager.MinecraftAccount;

public interface ClientSessionAdapter {
    SessionSwitchResult switchTo(Object minecraftClient, MinecraftAccount account);
}
