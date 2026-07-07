package dev.codex.altmanager.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AltManagerFabricConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private String microsoftClientId = "";

    public static AltManagerFabricConfig load(Path path) {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            if (!Files.exists(path)) {
                AltManagerFabricConfig config = new AltManagerFabricConfig();
                config.save(path);
                return config;
            }
            BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
            try {
                AltManagerFabricConfig config = GSON.fromJson(reader, AltManagerFabricConfig.class);
                return config == null ? new AltManagerFabricConfig() : config;
            } finally {
                reader.close();
            }
        } catch (IOException exception) {
            return new AltManagerFabricConfig();
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
