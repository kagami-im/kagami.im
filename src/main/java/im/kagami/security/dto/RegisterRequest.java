package im.kagami.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Registration payload; {@code nickname} is optional and falls back to the username. */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 32) String username,
        @NotBlank @Size(min = 8, max = 64) String password,
        @Size(max = 32) String nickname) {
}
