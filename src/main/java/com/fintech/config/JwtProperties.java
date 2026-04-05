package com.fintech.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.jwt")
@Validated
@Getter
@Setter
public class JwtProperties {

    // HS256(symmetric) secret key
    // For multiple services RSA256(asymmetric) would be preferred sp services can verify token without holding the signing key
    @NotBlank
    private String secret;

    @Min(60)
    private long expirationSeconds = 3600;
}
