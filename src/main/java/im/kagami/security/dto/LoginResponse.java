package im.kagami.security.dto;

/**
 * Successful login response. The frontend stores {@code accessToken} and sends it in the
 * Authorization header (Bearer prefix); {@code expiresIn} is in seconds and mirrors
 * {@code jwt.expiration-in}.
 */
public record LoginResponse(String accessToken, long expiresIn) {
}
