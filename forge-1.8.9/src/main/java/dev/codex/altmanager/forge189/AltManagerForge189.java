package dev.codex.altmanager.forge189;

import dev.codex.altmanager.AltManagerService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.nio.file.Path;

@Mod(
        modid = AltManagerForge189.MOD_ID,
        name = "Minecraft Alt Manager",
        version = AltManagerForge189.VERSION,
        clientSideOnly = true,
        acceptedMinecraftVersions = "[1.8.9]"
)
public final class AltManagerForge189 {
    public static final String MOD_ID = "altmanager";
    public static final String VERSION = "0.1.3";
    private static final int ACCOUNTS_BUTTON_ID = 89189;

    private static AltManagerForge189Config config;
    private static AltManagerService service;
    private static Forge189SessionAdapter sessionAdapter;

    @EventHandler
    public void init(FMLInitializationEvent event) {
        service();
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static synchronized AltManagerService service() {
        if (service != null) {
            return service;
        }
        config = AltManagerForge189Config.load(configDirectory().resolve("config.json"));
        Path accountsPath = configDirectory().resolve("accounts.json");
        if (config.hasMicrosoftClientId()) {
            service = AltManagerService.createDefault(accountsPath, config.getMicrosoftClientId());
        } else {
            service = AltManagerService.createAccessTokenOnly(accountsPath);
        }
        return service;
    }

    public static synchronized AltManagerForge189Config config() {
        if (config == null) {
            service();
        }
        return config;
    }

    public static synchronized Forge189SessionAdapter sessionAdapter() {
        if (sessionAdapter == null) {
            sessionAdapter = new Forge189SessionAdapter();
        }
        return sessionAdapter;
    }

    public static Path configDirectory() {
        return Minecraft.getMinecraft().mcDataDir.toPath().resolve("config").resolve("minecraft-alt-manager");
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiMultiplayer)) {
            return;
        }
        int x = event.gui.width - 112;
        event.buttonList.add(new GuiButton(ACCOUNTS_BUTTON_ID, x, 8, 104, 20, I18n.format("altmanager.button.accounts")));
    }

    @SubscribeEvent
    public void onGuiAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.button.id == ACCOUNTS_BUTTON_ID && event.gui instanceof GuiMultiplayer) {
            Minecraft.getMinecraft().displayGuiScreen(new AltManagerGui189(event.gui));
        }
    }
}
