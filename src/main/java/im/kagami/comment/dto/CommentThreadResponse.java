package im.kagami.comment.dto;

import im.kagami.comment.domain.Comment;
import im.kagami.post.dto.PostAuthorResponse;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * One comment as returned by the public API. Top-level threads carry their flat reply list; replies
 * themselves have an empty list.
 */
public record CommentThreadResponse(
        Long id,
        String content,
        OffsetDateTime createdAt,
        PostAuthorResponse author,
        List<CommentThreadResponse> replies
) {

    public static CommentThreadResponse of(Comment comment, PostAuthorResponse author, List<CommentThreadResponse> replies) {
        return new CommentThreadResponse(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                author,
                replies
        );
    }
}
