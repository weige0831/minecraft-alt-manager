package dev.codex.altmanager.fabric;

import dev.codex.altmanager.AltManagerService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class AltManagerFabricClient implements ClientModInitializer {
    private static AltManagerFabricConfig config;
    private static AltManagerService service;
    private static FabricClientSessionAdapter sessionAdapter;

    @Override
    public void onInitializeClient() {
        service();
    }

    public static synchronized AltManagerService service() {
        if (service != null) {
            return service;
        }
        config = AltManagerFabricConfig.load(configDirectory().resolve("config.json"));
        Path accountsPath = configDirectory().resolve("accounts.json");
        if (config.hasMicrosoftClientId()) {
            service = AltManagerService.createDefault(accountsPath, config.getMicrosoftClientId());
        } else {
            service = AltManagerService.createAccessTokenOnly(accountsPath);
        }
        return service;
    }

    public static synchronized AltManagerFabricConfig config() {
        if (config == null) {
            service();
        }
        return config;
    }

    public static synchronized FabricClientSessionAdapter sessionAdapter() {
        if (sessionAdapter == null) {
            sessionAdapter = new FabricClientSessionAdapter();
        }
        return sessionAdapter;
    }

    public static Path configDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve("minecraft-alt-manager");
    }
}
