package im.kagami.common.security;

/**
 * SecurityContext principal holding immutable identity only, so deliberately no password hash and
 * no mutable profile fields.
 */
public record CurrentUser(Long id, String username) {
}
