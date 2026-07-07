package dev.codex.altmanager.forge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AltManagerForgeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private String microsoftClientId = "";

    public static AltManagerForgeConfig load(Path path) {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            if (!Files.exists(path)) {
                AltManagerForgeConfig config = new AltManagerForgeConfig();
                config.save(path);
                return config;
            }
            BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
            try {
                AltManagerForgeConfig config = GSON.fromJson(reader, AltManagerForgeConfig.class);
                return config == null ? new AltManagerForgeConfig() : config;
            } finally {
                reader.close();
            }
        } catch (IOException exception) {
            return new AltManagerForgeConfig();
        }
    }

    public String getMicrosoftClientId() {
        return microsoftClientId == null ? "" : microsoftClientId.trim();
    }

    public boolean hasMicrosoftClientId() {
        return !getMicrosoftClientId().isEmpty();
    }

    private void save(Path path) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
        try {
            GSON.toJson(this, writer);
        } finally {
            writer.close();
        }
    }
}
