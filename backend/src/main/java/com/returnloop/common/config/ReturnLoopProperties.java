package com.returnloop.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Externalised application configuration. Every value has an environment
 * variable override so no secret needs to live in the repository.
 */
@ConfigurationProperties(prefix = "returnloop")
public class ReturnLoopProperties {

    private final Security security = new Security();
    private final Messaging messaging = new Messaging();
    private final Recovery recovery = new Recovery();
    private final MapFeature map = new MapFeature();
    private final Seed seed = new Seed();

    public Security getSecurity() {
        return security;
    }

    public Messaging getMessaging() {
        return messaging;
    }

    public Recovery getRecovery() {
        return recovery;
    }

    public MapFeature getMap() {
        return map;
    }

    public Seed getSeed() {
        return seed;
    }

    public static class Security {
        private final Jwt jwt = new Jwt();
        private List<String> corsAllowedOrigins = List.of("http://localhost:5173");

        public Jwt getJwt() {
            return jwt;
        }

        public List<String> getCorsAllowedOrigins() {
            return corsAllowedOrigins;
        }

        public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
            this.corsAllowedOrigins = corsAllowedOrigins;
        }

        public static class Jwt {
            private String secret;
            private String accessTokenTtl = "PT8H";

            public String getSecret() {
                return secret;
            }

            public void setSecret(String secret) {
                this.secret = secret;
            }

            public String getAccessTokenTtl() {
                return accessTokenTtl;
            }

            public void setAccessTokenTtl(String accessTokenTtl) {
                this.accessTokenTtl = accessTokenTtl;
            }
        }
    }

    public static class Messaging {
        private boolean kafkaEnabled = false;
        private String topic = "returnloop.events";

        public boolean isKafkaEnabled() {
            return kafkaEnabled;
        }

        public void setKafkaEnabled(boolean kafkaEnabled) {
            this.kafkaEnabled = kafkaEnabled;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }
    }

    public static class Recovery {
        private String strategy = "rule-based";

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }
    }

    public static class MapFeature {
        private String provider = "simulated";
        private String apiKey;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public boolean isSimulated() {
            return "simulated".equalsIgnoreCase(provider) || apiKey == null || apiKey.isBlank();
        }
    }

    public static class Seed {
        private boolean enabled = true;
        private String demoPassword = "ReturnLoop!2026";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDemoPassword() {
            return demoPassword;
        }

        public void setDemoPassword(String demoPassword) {
            this.demoPassword = demoPassword;
        }
    }
}
