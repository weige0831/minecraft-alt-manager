package dev.codex.altmanager.forge112;

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
        modid = AltManagerForge112.MOD_ID,
        name = "Minecraft Alt Manager",
        version = AltManagerForge112.VERSION,
        clientSideOnly = true,
        acceptedMinecraftVersions = "[1.12.2]"
)
public final class AltManagerForge112 {
    public static final String MOD_ID = "altmanager";
    public static final String VERSION = "0.1.3";
    private static final int ACCOUNTS_BUTTON_ID = 891201;

    private static AltManagerForge112Config config;
    private static AltManagerService service;
    private static Forge112SessionAdapter sessionAdapter;

    @EventHandler
    public void init(FMLInitializationEvent event) {
        service();
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static synchronized AltManagerService service() {
        if (service != null) {
            return service;
        }
        config = AltManagerForge112Config.load(configDirectory().resolve("config.json"));
        Path accountsPath = configDirectory().resolve("accounts.json");
        if (config.hasMicrosoftClientId()) {
            service = AltManagerService.createDefault(accountsPath, config.getMicrosoftClientId());
        } else {
            service = AltManagerService.createAccessTokenOnly(accountsPath);
        }
        return service;
    }

    public static synchronized AltManagerForge112Config config() {
        if (config == null) {
            service();
        }
        return config;
    }

    public static synchronized Forge112SessionAdapter sessionAdapter() {
        if (sessionAdapter == null) {
            sessionAdapter = new Forge112SessionAdapter();
        }
        return sessionAdapter;
    }

    public static Path configDirectory() {
        return Minecraft.getMinecraft().mcDataDir.toPath().resolve("config").resolve("minecraft-alt-manager");
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiMultiplayer)) {
            return;
        }
        int x = event.getGui().width - 112;
        event.getButtonList().add(new GuiButton(ACCOUNTS_BUTTON_ID, x, 8, 104, 20, I18n.format("altmanager.button.accounts")));
    }

    @SubscribeEvent
    public void onGuiAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.getButton().id == ACCOUNTS_BUTTON_ID && event.getGui() instanceof GuiMultiplayer) {
            Minecraft.getMinecraft().displayGuiScreen(new AltManagerGui112(event.getGui()));
        }
    }
}
