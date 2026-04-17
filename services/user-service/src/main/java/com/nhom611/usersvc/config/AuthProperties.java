package com.nhom611.usersvc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private final Jwt jwt = new Jwt();
    private final Refresh refresh = new Refresh();
    private final Cors cors = new Cors();

    public Jwt getJwt() {
        return jwt;
    }

    public Refresh getRefresh() {
        return refresh;
    }

    public Cors getCors() {
        return cors;
    }

    public static class Jwt {
        private String secret;
        private long accessTtlSeconds = 3600;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessTtlSeconds() {
            return accessTtlSeconds;
        }

        public void setAccessTtlSeconds(long accessTtlSeconds) {
            this.accessTtlSeconds = accessTtlSeconds;
        }
    }

    public static class Refresh {
        private long ttlSeconds = 7 * 24 * 60 * 60;
        private final Cookie cookie = new Cookie();

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }

        public Cookie getCookie() {
            return cookie;
        }

        public static class Cookie {
            private String name = "refresh_token";
            private String path = "/auth";
            private boolean secure = false;
            private String sameSite = "Lax";

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getPath() {
                return path;
            }

            public void setPath(String path) {
                this.path = path;
            }

            public boolean isSecure() {
                return secure;
            }

            public void setSecure(boolean secure) {
                this.secure = secure;
            }

            public String getSameSite() {
                return sameSite;
            }

            public void setSameSite(String sameSite) {
                this.sameSite = sameSite;
            }
        }
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }
}
