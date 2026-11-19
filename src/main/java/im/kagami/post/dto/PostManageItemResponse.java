package im.kagami.post.dto;

import im.kagami.post.domain.Post;
import im.kagami.post.domain.Tag;

import java.time.OffsetDateTime;
import java.util.List;

/** One row of the author's own management list: workflow fields first, no content. */
public record PostManageItemResponse(
        Long id,
        String title,
        String slug,
        /** 0 = draft, 1 = published. */
        Integer status,
        Boolean pinned,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<PostTagResponse> tags
) {

    public static PostManageItemResponse from(Post post, List<Tag> tags) {
        return new PostManageItemResponse(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getStatus(),
                post.getPinned(),
                post.getPublishedAt(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                tags.stream().map(PostTagResponse::from).toList());
    }
}
