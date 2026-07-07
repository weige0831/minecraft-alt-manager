package dev.codex.altmanager;

import dev.codex.altmanager.util.Uuids;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class MinecraftAccount {
    private final String username;
    private final String uuid;
    private final String accessToken;
    private final String refreshToken;
    private final String xuid;
    private final String clientId;
    private final Instant accessTokenExpiresAt;
    private final AccountKind kind;

    private MinecraftAccount(Builder builder) {
        this.username = requireNonBlank(builder.username, "username");
        this.uuid = Uuids.normalize(requireNonBlank(builder.uuid, "uuid"));
        this.accessToken = requireNonBlank(builder.accessToken, "accessToken");
        this.refreshToken = emptyToNull(builder.refreshToken);
        this.xuid = emptyToNull(builder.xuid);
        this.clientId = emptyToNull(builder.clientId);
        this.accessTokenExpiresAt = builder.accessTokenExpiresAt;
        this.kind = builder.kind == null ? AccountKind.ACCESS_TOKEN : builder.kind;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUsername() {
        return username;
    }

    public String getUuid() {
        return uuid;
    }

    public String getUuidNoDashes() {
        return Uuids.stripDashes(uuid);
    }

    public UUID getUuidAsUuid() {
        return UUID.fromString(uuid);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getXuid() {
        return xuid;
    }

    public String getClientId() {
        return clientId;
    }

    public Instant getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public AccountKind getKind() {
        return kind;
    }

    public boolean hasRefreshToken() {
        return refreshToken != null;
    }

    public boolean isAccessTokenExpired(Instant now) {
        return accessTokenExpiresAt != null && !accessTokenExpiresAt.isAfter(now);
    }

    public Builder toBuilder() {
        return builder()
                .username(username)
                .uuid(uuid)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .xuid(xuid)
                .clientId(clientId)
                .accessTokenExpiresAt(accessTokenExpiresAt)
                .kind(kind);
    }

    @Override
    public String toString() {
        return "MinecraftAccount{" +
                "username='" + username + '\'' +
                ", uuid='" + uuid + '\'' +
                ", kind=" + kind +
                ", accessTokenExpiresAt=" + accessTokenExpiresAt +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MinecraftAccount)) {
            return false;
        }
        MinecraftAccount other = (MinecraftAccount) obj;
        return uuid.equals(other.uuid) && kind == other.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, kind);
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }

    public static final class Builder {
        private String username;
        private String uuid;
        private String accessToken;
        private String refreshToken;
        private String xuid;
        private String clientId;
        private Instant accessTokenExpiresAt;
        private AccountKind kind;

        private Builder() {
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder xuid(String xuid) {
            this.xuid = xuid;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder accessTokenExpiresAt(Instant accessTokenExpiresAt) {
            this.accessTokenExpiresAt = accessTokenExpiresAt;
            return this;
        }

        public Builder kind(AccountKind kind) {
            this.kind = kind;
            return this;
        }

        public MinecraftAccount build() {
            return new MinecraftAccount(this);
        }
    }
}
