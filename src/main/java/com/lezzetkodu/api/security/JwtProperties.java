package com.lezzetkodu.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** application.yml'deki app.jwt.* ayarlari. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration) {
}
