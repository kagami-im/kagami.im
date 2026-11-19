package im.kagami.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/posts/{slugOrId}/comments. parentId selects the top-level comment to reply to;
 * null creates a top-level comment.
 */
public record CreateCommentRequest(
        @NotBlank @Size(max = 1000) String content,
        Long parentId
) {
}
