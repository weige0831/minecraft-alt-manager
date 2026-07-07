package dev.codex.altmanager.store;

import dev.codex.altmanager.MinecraftAccount;

import java.io.IOException;

public final class InlineCredentialStore implements CredentialStore {
    @Override
    public MinecraftAccount loadCredentials(MinecraftAccount account) throws IOException {
        return account;
    }

    @Override
    public MinecraftAccount saveCredentials(MinecraftAccount account) throws IOException {
        return account;
    }

    @Override
    public void removeCredentials(String uuid) throws IOException {
        // Credentials are stored inline by JsonAccountStore in the first implementation.
    }
}
