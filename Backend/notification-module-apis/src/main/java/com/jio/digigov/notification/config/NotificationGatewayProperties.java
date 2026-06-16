package com.jio.digigov.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "notification.gateway")
public class NotificationGatewayProperties {

    private Credentials credentials;
    private Endpoints endpoints;

    @Data
    public static class Credentials {
        private Admin admin;

        @Data
        public static class Admin {
            private String clientId;
            private String clientSecret;
        }
    }

    @Data
    public static class Endpoints {
        private String token;
        private Notification notification;
        private Otp otp;

        @Data
        public static class Notification {
            private String onboard;
            private String approve;
            private String send;
        }

        @Data
        public static class Otp {
            private String onboard;
            private String approve;
            private String init;
            private String verify;
        }
    }
}