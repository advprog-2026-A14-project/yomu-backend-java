package id.ac.ui.cs.advprog.yomubackendjava.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, @DefaultValue("86400") long ttlSeconds) {
}
