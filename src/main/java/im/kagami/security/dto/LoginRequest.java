package im.kagami.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login payload
 *
 * @param username login name, not blank and at most 64 characters
 * @param password plaintext password, not blank and at most 128 characters
 */
public record LoginRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(max = 128) String password) {
}
