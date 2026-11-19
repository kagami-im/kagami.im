package im.kagami.comment.dto;

import im.kagami.comment.domain.Comment;
import im.kagami.post.dto.PostAuthorResponse;

import java.time.OffsetDateTime;

/** One comment as seen by the post author in moderation, including hidden ones. */
public record CommentManageItemResponse(
        Long id,
        Long parentId,
        String content,
        /** 0 = visible, 1 = hidden. */
        Integer status,
        OffsetDateTime createdAt,
        PostAuthorResponse author
) {

    public static CommentManageItemResponse from(Comment comment, PostAuthorResponse author) {
        return new CommentManageItemResponse(
                comment.getId(),
                comment.getParentId(),
                comment.getContent(),
                comment.getStatus(),
                comment.getCreatedAt(),
                author
        );
    }
}
