package im.kagami.post.dto;

import im.kagami.post.domain.Post;
import im.kagami.post.domain.Tag;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Published post as shown on public list pages. Carries no content — summaries keep the list light
 * and the full Markdown only travels on the detail endpoint. The status is not part of the shape:
 * everything here is published by definition.
 */
public record PostSummaryResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String coverUrl,
        Boolean pinned,
        Integer readingMinutes,
        OffsetDateTime publishedAt,
        PostAuthorResponse author,
        List<PostTagResponse> tags
) {

    public static PostSummaryResponse from(Post post, PostAuthorResponse author, List<Tag> tags) {
        return new PostSummaryResponse(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getSummary(),
                post.getCoverUrl(),
                post.getPinned(),
                post.getReadingMinutes(),
                post.getPublishedAt(),
                author,
                tags.stream().map(PostTagResponse::from).toList());
    }
}
