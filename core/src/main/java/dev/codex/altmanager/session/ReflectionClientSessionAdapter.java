package dev.codex.altmanager.session;

import dev.codex.altmanager.MinecraftAccount;

public final class ReflectionClientSessionAdapter implements ClientSessionAdapter {
    private final MinecraftSessionInjector injector;

    public ReflectionClientSessionAdapter() {
        this(new MinecraftSessionInjector());
    }

    public ReflectionClientSessionAdapter(MinecraftSessionInjector injector) {
        this.injector = injector;
    }

    @Override
    public SessionSwitchResult switchTo(Object minecraftClient, MinecraftAccount account) {
        InjectionResult result = injector.inject(minecraftClient, account);
        if (result.isSuccess()) {
            return SessionSwitchResult.success(result.getMessage());
        }
        return SessionSwitchResult.failure(SessionSwitchStatus.SESSION_NOT_FOUND, result.getMessage(), result.getCause());
    }
}
