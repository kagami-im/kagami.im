package im.kagami.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body of {@code PUT /api/manage/posts/{id}}. Deliberately does not carry {@code status}: the
 * workflow transition lives on the publish endpoint, so an accidental edit cannot publish a draft.
 */
public record UpdatePostRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 500) String summary,
        @Size(max = 150)
        @Pattern(regexp = CreatePostRequest.SLUG_PATTERN,
                message = "Slug may only contain lowercase letters, digits and dashes")
        String slug,
        @Size(max = 500) String coverUrl,
        @NotBlank String content,
        /** Pin to the top of the public list; defaults to false. */
        Boolean pinned,
        @Size(max = 10) List<@NotBlank @Size(max = 30) String> tags
) {
}
