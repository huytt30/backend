package com.da.billiards.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Cors cors,
        Admin admin,
        String uploadDir,
        boolean seedSampleData) {

    public record Jwt(String secret, Duration expiration) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Admin(String email, String password) {
    }
}
