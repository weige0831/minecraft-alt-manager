package dev.codex.altmanager.forge;

import dev.codex.altmanager.AltManagerService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

@Mod(AltManagerForgeClient.MOD_ID)
public final class AltManagerForgeClient {
    public static final String MOD_ID = "altmanager";

    private static AltManagerForgeConfig config;
    private static AltManagerService service;
    private static ForgeClientSessionAdapter sessionAdapter;

    public AltManagerForgeClient() {
        service();
        MinecraftForge.EVENT_BUS.addListener(this::onScreenInit);
    }

    public static synchronized AltManagerService service() {
        if (service != null) {
            return service;
        }
        config = AltManagerForgeConfig.load(configDirectory().resolve("config.json"));
        Path accountsPath = configDirectory().resolve("accounts.json");
        if (config.hasMicrosoftClientId()) {
            service = AltManagerService.createDefault(accountsPath, config.getMicrosoftClientId());
        } else {
            service = AltManagerService.createAccessTokenOnly(accountsPath);
        }
        return service;
    }

    public static synchronized AltManagerForgeConfig config() {
        if (config == null) {
            service();
        }
        return config;
    }

    public static synchronized ForgeClientSessionAdapter sessionAdapter() {
        if (sessionAdapter == null) {
            sessionAdapter = new ForgeClientSessionAdapter();
        }
        return sessionAdapter;
    }

    public static Path configDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve("minecraft-alt-manager");
    }

    private void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof JoinMultiplayerScreen)) {
            return;
        }
        int x = event.getScreen().width - 112;
        int y = 8;
        event.addListener(Button.builder(Component.translatable("altmanager.button.accounts"), button ->
                Minecraft.getInstance().setScreen(new AltManagerScreen(event.getScreen()))
        ).bounds(x, y, 104, 20).build());
    }
}
