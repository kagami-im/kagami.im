package im.kagami.post.dto;

import im.kagami.post.domain.Post;
import im.kagami.post.domain.Tag;

import java.time.OffsetDateTime;
import java.util.List;

/** Full post for the editor, any status; returned by GET/POST/PUT on the manage API. */
public record PostManageDetailResponse(
        Long id,
        String title,
        String slug,
        String summary,
        String coverUrl,
        String content,
        /** 0 = draft, 1 = published. */
        Integer status,
        Boolean pinned,
        Integer wordCount,
        Integer readingMinutes,
        OffsetDateTime publishedAt,
        List<PostTagResponse> tags,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static PostManageDetailResponse from(Post post, List<Tag> tags) {
        return new PostManageDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getSummary(),
                post.getCoverUrl(),
                post.getContent(),
                post.getStatus(),
                post.getPinned(),
                post.getWordCount(),
                post.getReadingMinutes(),
                post.getPublishedAt(),
                tags.stream().map(PostTagResponse::from).toList(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
