package com.tripnexa.smarttrip.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private Duration expiration = Duration.ofHours(2);

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public Duration getExpiration() { return expiration; }
    public void setExpiration(Duration expiration) { this.expiration = expiration; }
}
