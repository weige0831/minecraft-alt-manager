package dev.codex.altmanager.auth;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MicrosoftAuthConfig {
    public static final String DEFAULT_TENANT = "consumers";

    private final String clientId;
    private final String tenant;
    private final List<String> scopes;
    private final boolean verifyEntitlements;

    private MicrosoftAuthConfig(Builder builder) {
        if (builder.clientId == null || builder.clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }
        this.clientId = builder.clientId;
        this.tenant = builder.tenant == null || builder.tenant.trim().isEmpty() ? DEFAULT_TENANT : builder.tenant;
        this.scopes = builder.scopes == null || builder.scopes.isEmpty()
                ? Collections.unmodifiableList(Arrays.asList("XboxLive.signin", "offline_access"))
                : Collections.unmodifiableList(new ArrayList<String>(builder.scopes));
        this.verifyEntitlements = builder.verifyEntitlements;
    }

    public static Builder builder(String clientId) {
        return new Builder(clientId);
    }

    public String getClientId() {
        return clientId;
    }

    public String getTenant() {
        return tenant;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public String getScopeString() {
        StringBuilder builder = new StringBuilder();
        for (String scope : scopes) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(scope);
        }
        return builder.toString();
    }

    public boolean isVerifyEntitlements() {
        return verifyEntitlements;
    }

    public String getDeviceCodeUrl() {
        return "https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/devicecode";
    }

    public String getTokenUrl() {
        return "https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/token";
    }

    public static final class Builder {
        private final String clientId;
        private String tenant;
        private List<String> scopes;
        private boolean verifyEntitlements = true;

        private Builder(String clientId) {
            this.clientId = clientId;
        }

        public Builder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public Builder scopes(List<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        public Builder verifyEntitlements(boolean verifyEntitlements) {
            this.verifyEntitlements = verifyEntitlements;
            return this;
        }

        public MicrosoftAuthConfig build() {
            return new MicrosoftAuthConfig(this);
        }
    }
}
