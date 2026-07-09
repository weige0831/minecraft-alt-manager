package dev.codex.altmanager.forge189;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AltManagerForge189Config {
    private static final String DEFAULT_MICROSOFT_CLIENT_ID = "e3c9f9be-7cde-49c9-887a-20cc3f3fa10c";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private String microsoftClientId = DEFAULT_MICROSOFT_CLIENT_ID;

    public static AltManagerForge189Config load(Path path) {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            if (!Files.exists(path)) {
                AltManagerForge189Config config = new AltManagerForge189Config();
                config.save(path);
                return config;
            }
            BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
            try {
                AltManagerForge189Config config = GSON.fromJson(reader, AltManagerForge189Config.class);
                return config == null ? new AltManagerForge189Config() : config;
            } finally {
                reader.close();
            }
        } catch (IOException exception) {
            return new AltManagerForge189Config();
        }
    }

    public String getMicrosoftClientId() {
        String configured = microsoftClientId == null ? "" : microsoftClientId.trim();
        return configured.isEmpty() ? DEFAULT_MICROSOFT_CLIENT_ID : configured;
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
