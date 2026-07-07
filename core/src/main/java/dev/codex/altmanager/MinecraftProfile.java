package dev.codex.altmanager;

import dev.codex.altmanager.util.Uuids;

public final class MinecraftProfile {
    private final String id;
    private final String name;

    public MinecraftProfile(String id, String name) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.id = Uuids.normalize(id);
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getIdNoDashes() {
        return Uuids.stripDashes(id);
    }

    public String getName() {
        return name;
    }
}
