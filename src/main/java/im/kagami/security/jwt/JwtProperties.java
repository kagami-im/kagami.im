package im.kagami.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings, bound to {@code jwt.*}. {@code tokenPrefix} is normally {@code "Bearer "} and is
 * stripped while parsing; {@code expirationIn} is in seconds.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String issuer, String header, String tokenPrefix, long expirationIn) {
}
