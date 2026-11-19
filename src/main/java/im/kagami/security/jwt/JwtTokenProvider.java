package im.kagami.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/** Signs and verifies JWTs with RS256: the private key signs, the public key verifies. */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    /** Custom claim holding the user id; signing and parsing must agree on the name. */
    public static final String CLAIM_USER_ID = "user_id";

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final JwtProperties jwtProperties;

    public String generateToken(Long userId, String username) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(username)
                    .issuer(jwtProperties.issuer())
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(jwtProperties.expirationIn(), ChronoUnit.SECONDS)))
                    .claim(CLAIM_USER_ID, userId)
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new RSASSASigner(privateKey));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    /**
     * @throws IllegalArgumentException when the signature is invalid, the token has expired or
     *                                  parsing failed; the filter responds by clearing the context
     */
    public JWTClaimsSet parseAndValidate(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new RSASSAVerifier(publicKey);

            if (!signedJWT.verify(verifier)) {
                throw new IllegalArgumentException("JWT signature verification failed");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            if (expiration == null || expiration.before(new Date())) {
                throw new IllegalArgumentException("JWT has expired");
            }
            return claims;
        } catch (ParseException | JOSEException e) {
            throw new IllegalArgumentException("Failed to parse JWT", e);
        }
    }
}
