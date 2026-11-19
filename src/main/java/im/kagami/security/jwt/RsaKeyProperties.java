package im.kagami.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * RSA key locations, bound to {@code jwt.rsa.*}. The private key is PKCS8, the public key X509, both
 * PEM encoded.
 */
@ConfigurationProperties(prefix = "jwt.rsa")
public record RsaKeyProperties(Resource privateKeyLocation, Resource publicKeyLocation) {
}
