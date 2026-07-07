package dev.codex.altmanager.session;

import dev.codex.altmanager.MinecraftAccount;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class MinecraftSessionInjector {
    private static final List<String> CLIENT_CLASS_NAMES = Arrays.asList(
            "net.minecraft.client.Minecraft",
            "net.minecraft.client.MinecraftClient"
    );
    private static final List<String> CLIENT_SINGLETON_METHODS = Arrays.asList(
            "getInstance",
            "getMinecraft",
            "func_71410_x",
            "method_1551"
    );
    private static final List<String> CLIENT_SINGLETON_FIELDS = Arrays.asList(
            "instance",
            "theMinecraft",
            "field_71432_P",
            "INSTANCE"
    );
    private static final List<String> SESSION_FIELD_NAMES = Arrays.asList(
            "session",
            "field_71449_j",
            "field_1726"
    );
    private static final List<String> SESSION_CLASS_NAMES = Arrays.asList(
            "net.minecraft.util.Session",
            "net.minecraft.client.util.Session",
            "net.minecraft.client.session.Session",
            "net.minecraft.client.User",
            "net.minecraft.class_320"
    );

    public InjectionResult injectCurrent(MinecraftAccount account) {
        for (String className : CLIENT_CLASS_NAMES) {
            Class<?> clientClass = tryClass(className);
            if (clientClass == null) {
                continue;
            }
            Object instance = findSingleton(clientClass);
            if (instance != null) {
                return inject(instance, account);
            }
        }
        return InjectionResult.failure("Could not locate a loaded Minecraft client singleton");
    }

    public InjectionResult inject(Object minecraftClient, MinecraftAccount account) {
        if (minecraftClient == null) {
            return InjectionResult.failure("Minecraft client instance must not be null");
        }
        if (account == null) {
            return InjectionResult.failure("Minecraft account must not be null");
        }

        List<FieldCandidate> candidates = findSessionFields(minecraftClient.getClass());
        if (candidates.isEmpty()) {
            return InjectionResult.failure("Could not find a likely Session field on " + minecraftClient.getClass().getName());
        }

        Throwable lastFailure = null;
        for (FieldCandidate candidate : candidates) {
            Field field = candidate.field;
            try {
                Object session = createSession(field.getType(), account);
                makeWritable(field);
                field.set(minecraftClient, session);
                return InjectionResult.success(
                        "Injected Minecraft session",
                        field.getType().getName(),
                        field.getName()
                );
            } catch (Throwable failure) {
                lastFailure = failure;
            }
        }
        return InjectionResult.failure("Found likely Session fields, but none accepted the account shape", lastFailure);
    }

    private Object findSingleton(Class<?> clientClass) {
        for (String methodName : CLIENT_SINGLETON_METHODS) {
            try {
                Method method = clientClass.getDeclaredMethod(methodName);
                if (!Modifier.isStatic(method.getModifiers()) || method.getParameterTypes().length != 0) {
                    continue;
                }
                method.setAccessible(true);
                Object value = method.invoke(null);
                if (value != null) {
                    return value;
                }
            } catch (ReflectiveOperationException ignored) {
                // Try the next common singleton symbol.
            }
        }

        for (String fieldName : CLIENT_SINGLETON_FIELDS) {
            try {
                Field field = clientClass.getDeclaredField(fieldName);
                if (!Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(null);
                if (value != null) {
                    return value;
                }
            } catch (ReflectiveOperationException ignored) {
                // Try the next common singleton symbol.
            }
        }
        return null;
    }

    private List<FieldCandidate> findSessionFields(Class<?> clientClass) {
        List<FieldCandidate> candidates = new ArrayList<FieldCandidate>();
        Class<?> current = clientClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                int score = scoreField(field);
                if (score > 0) {
                    candidates.add(new FieldCandidate(field, score));
                }
            }
            current = current.getSuperclass();
        }
        candidates.sort(new Comparator<FieldCandidate>() {
            @Override
            public int compare(FieldCandidate left, FieldCandidate right) {
                return Integer.compare(right.score, left.score);
            }
        });
        return candidates;
    }

    private int scoreField(Field field) {
        int score = 0;
        String fieldName = field.getName();
        Class<?> type = field.getType();
        String typeName = type.getName();
        String simpleName = type.getSimpleName();

        if (SESSION_FIELD_NAMES.contains(fieldName)) {
            score += 100;
        }
        if (SESSION_CLASS_NAMES.contains(typeName)) {
            score += 80;
        }
        if ("Session".equals(simpleName) || typeName.endsWith(".Session")) {
            score += 40;
        }
        if (hasSupportedConstructor(type)) {
            score += 20;
        }
        return score;
    }

    private boolean hasSupportedConstructor(Class<?> sessionClass) {
        Constructor<?>[] constructors = sessionClass.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            if (buildArguments(constructor.getParameterTypes(), null) != null) {
                return true;
            }
        }
        return false;
    }

    private Object createSession(Class<?> sessionClass, MinecraftAccount account) throws ReflectiveOperationException {
        Constructor<?>[] constructors = sessionClass.getDeclaredConstructors();
        Arrays.sort(constructors, new Comparator<Constructor<?>>() {
            @Override
            public int compare(Constructor<?> left, Constructor<?> right) {
                return Integer.compare(right.getParameterTypes().length, left.getParameterTypes().length);
            }
        });
        for (Constructor<?> constructor : constructors) {
            Object[] arguments = buildArguments(constructor.getParameterTypes(), account);
            if (arguments == null) {
                continue;
            }
            constructor.setAccessible(true);
            return constructor.newInstance(arguments);
        }
        throw new NoSuchMethodException("No supported Session constructor found on " + sessionClass.getName());
    }

    private Object[] buildArguments(Class<?>[] parameterTypes, MinecraftAccount account) {
        if (parameterTypes.length == 4
                && parameterTypes[0] == String.class
                && parameterTypes[1] == String.class
                && parameterTypes[2] == String.class
                && (parameterTypes[3] == String.class || parameterTypes[3].isEnum())) {
            if (account == null) {
                return new Object[4];
            }
            return new Object[]{
                    account.getUsername(),
                    account.getUuidNoDashes(),
                    account.getAccessToken(),
                    parameterTypes[3] == String.class ? "msa" : enumValue(parameterTypes[3])
            };
        }

        if (parameterTypes.length == 6
                && parameterTypes[0] == String.class
                && parameterTypes[1] == UUID.class
                && parameterTypes[2] == String.class
                && parameterTypes[3] == Optional.class
                && parameterTypes[4] == Optional.class
                && parameterTypes[5].isEnum()) {
            if (account == null) {
                return new Object[6];
            }
            return new Object[]{
                    account.getUsername(),
                    account.getUuidAsUuid(),
                    account.getAccessToken(),
                    optional(account.getXuid()),
                    optional(account.getClientId()),
                    enumValue(parameterTypes[5])
            };
        }

        Object[] arguments = new Object[parameterTypes.length];
        int stringIndex = 0;
        int optionalIndex = 0;
        boolean hasUuidParameter = contains(parameterTypes, UUID.class);
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            if (type == String.class) {
                if (account == null) {
                    arguments[i] = "";
                    continue;
                }
                arguments[i] = stringArgument(account, i, stringIndex++, hasUuidParameter);
            } else if (type == UUID.class) {
                arguments[i] = account == null ? new UUID(0L, 0L) : account.getUuidAsUuid();
            } else if (type == Optional.class) {
                String value = null;
                if (account != null) {
                    value = optionalIndex++ == 0 ? account.getXuid() : account.getClientId();
                }
                arguments[i] = optional(value);
            } else if (type.isEnum()) {
                arguments[i] = enumValue(type);
            } else if ("com.mojang.authlib.GameProfile".equals(type.getName())) {
                arguments[i] = account == null ? null : createGameProfile(type, account);
            } else {
                return null;
            }
        }
        return arguments;
    }

    private String stringArgument(MinecraftAccount account, int parameterIndex, int stringIndex, boolean hasUuidParameter) {
        if (parameterIndex == 0 || stringIndex == 0) {
            return account.getUsername();
        }
        if (hasUuidParameter && stringIndex == 1) {
            return account.getAccessToken();
        }
        if (stringIndex == 1) {
            return account.getUuidNoDashes();
        }
        if (stringIndex == 2) {
            return account.getAccessToken();
        }
        return "msa";
    }

    private Object createGameProfile(Class<?> type, MinecraftAccount account) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor(UUID.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(account.getUuidAsUuid(), account.getUsername());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("Could not construct GameProfile", exception);
        }
    }

    private Object enumValue(Class<?> enumType) {
        Object[] constants = enumType.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return null;
        }
        for (String preferred : Arrays.asList("MSA", "MICROSOFT", "MOJANG")) {
            for (Object constant : constants) {
                if (((Enum<?>) constant).name().equalsIgnoreCase(preferred)) {
                    return constant;
                }
            }
        }
        return constants[0];
    }

    private Optional<String> optional(String value) {
        return value == null || value.trim().isEmpty() ? Optional.<String>empty() : Optional.of(value);
    }

    private boolean contains(Class<?>[] types, Class<?> expected) {
        for (Class<?> type : types) {
            if (type == expected) {
                return true;
            }
        }
        return false;
    }

    private void makeWritable(Field field) {
        field.setAccessible(true);
        int modifiers = field.getModifiers();
        if (!Modifier.isFinal(modifiers)) {
            return;
        }
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, modifiers & ~Modifier.FINAL);
        } catch (ReflectiveOperationException ignored) {
            // On newer JVMs this private field may not exist. Field#set can still work for many non-static finals.
        }
    }

    private Class<?> tryClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static final class FieldCandidate {
        private final Field field;
        private final int score;

        private FieldCandidate(Field field, int score) {
            this.field = field;
            this.score = score;
        }
    }
}
