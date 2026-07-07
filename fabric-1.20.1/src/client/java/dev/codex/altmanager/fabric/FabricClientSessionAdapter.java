package dev.codex.altmanager.fabric;

import dev.codex.altmanager.MinecraftAccount;
import dev.codex.altmanager.session.ClientSessionAdapter;
import dev.codex.altmanager.session.InjectionResult;
import dev.codex.altmanager.session.MinecraftSessionInjector;
import dev.codex.altmanager.session.SessionSwitchResult;
import dev.codex.altmanager.session.SessionSwitchStatus;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;

public final class FabricClientSessionAdapter implements ClientSessionAdapter {
    private final MinecraftSessionInjector injector = new MinecraftSessionInjector();

    @Override
    public SessionSwitchResult switchTo(Object minecraftClient, MinecraftAccount account) {
        if (!(minecraftClient instanceof MinecraftClient)) {
            return SessionSwitchResult.failure(SessionSwitchStatus.FAILED, "Expected MinecraftClient for Fabric 1.20.1");
        }

        MinecraftClient client = (MinecraftClient) minecraftClient;
        if (client.world != null || client.getNetworkHandler() != null) {
            return SessionSwitchResult.failure(
                    SessionSwitchStatus.REQUIRES_DISCONNECT,
                    "Disconnect before switching accounts"
            );
        }

        InjectionResult result = injector.inject(client, account);
        if (!result.isSuccess()) {
            return SessionSwitchResult.failure(SessionSwitchStatus.SESSION_NOT_FOUND, result.getMessage(), result.getCause());
        }

        try {
            clearLikelyProfileKeyCaches(client);
        } catch (RuntimeException exception) {
            return SessionSwitchResult.failure(
                    SessionSwitchStatus.PROFILE_KEY_REFRESH_FAILED,
                    "Session changed, but profile key caches could not be cleared",
                    exception
            );
        }
        return SessionSwitchResult.success("Switched to " + account.getUsername());
    }

    private void clearLikelyProfileKeyCaches(MinecraftClient client) {
        Class<?> current = client.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                    continue;
                }
                if (!isUserSessionCache(field)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    field.set(client, null);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Could not clear " + field.getName(), exception);
                }
            }
            current = current.getSuperclass();
        }
    }

    private boolean isUserSessionCache(Field field) {
        String name = field.getName().toLowerCase(Locale.ROOT);
        String type = field.getType().getName().toLowerCase(Locale.ROOT);
        return name.contains("profilekey")
                || name.contains("userapiservice")
                || name.contains("userproperties")
                || type.contains("profilekey")
                || type.contains("userapiservice");
    }
}
