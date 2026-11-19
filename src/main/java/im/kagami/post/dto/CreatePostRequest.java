package im.kagami.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body of {@code POST /api/manage/posts}. Published state is not part of the payload: posts are born
 * as drafts and move through the explicit publish endpoint.
 */
public record CreatePostRequest(
        @NotBlank @Size(max = 150) String title,
        /** Optional short excerpt; blank becomes null. */
        @Size(max = 500) String summary,
        /** Optional SEO-friendly URL segment; {@code null} addresses the post by id instead. */
        @Size(max = 150)
        @Pattern(regexp = SLUG_PATTERN, message = "Slug may only contain lowercase letters, digits and dashes")
        String slug,
        @Size(max = 500) String coverUrl,
        /** Raw Markdown; blank is rejected even for drafts. */
        @NotBlank String content,
        /** Pin to the top of the public list; defaults to false. */
        Boolean pinned,
        /** At most 10 raw tag names, each up to 30 chars; duplicates are collapsed by the service. */
        @Size(max = 10) List<@NotBlank @Size(max = 30) String> tags
) {

    /** Shape of a URL segment; pure digits are additionally rejected by the service (id namespace). */
    public static final String SLUG_PATTERN = "^[a-z0-9]+(-[a-z0-9]+)*$";
}
