package dev.codex.altmanager.store;

import dev.codex.altmanager.MinecraftAccount;

import java.io.IOException;

public interface CredentialStore {
    MinecraftAccount loadCredentials(MinecraftAccount account) throws IOException;

    MinecraftAccount saveCredentials(MinecraftAccount account) throws IOException;

    void removeCredentials(String uuid) throws IOException;
}
