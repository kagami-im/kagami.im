package im.kagami.post.dto;

import im.kagami.post.domain.Post;
import im.kagami.post.domain.Tag;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Published post as shown on the public detail page, including the raw Markdown in {@code content};
 * the client renders it to HTML.
 */
public record PostDetailResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String coverUrl,
        String content,
        Boolean pinned,
        Integer wordCount,
        Integer readingMinutes,
        OffsetDateTime publishedAt,
        OffsetDateTime updatedAt,
        PostAuthorResponse author,
        List<PostTagResponse> tags
) {

    public static PostDetailResponse from(Post post, PostAuthorResponse author, List<Tag> tags) {
        return new PostDetailResponse(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getSummary(),
                post.getCoverUrl(),
                post.getContent(),
                post.getPinned(),
                post.getWordCount(),
                post.getReadingMinutes(),
                post.getPublishedAt(),
                post.getUpdatedAt(),
                author,
                tags.stream().map(PostTagResponse::from).toList());
    }
}
