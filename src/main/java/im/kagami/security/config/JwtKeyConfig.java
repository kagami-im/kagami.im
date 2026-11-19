package im.kagami.security.config;

import im.kagami.security.jwt.RsaKeyProperties;
import im.kagami.security.util.PemUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/** Makes the RSA keys available as beans so {@code JwtTokenProvider} can sign and verify. */
@Configuration
@RequiredArgsConstructor
public class JwtKeyConfig {

    private final RsaKeyProperties props;

    @Bean
    public RSAPrivateKey rsaPrivateKey() throws Exception {
        return PemUtils.readPrivateKey(props.privateKeyLocation());
    }

    @Bean
    public RSAPublicKey rsaPublicKey() throws Exception {
        return PemUtils.readPublicKey(props.publicKeyLocation());
    }
}
