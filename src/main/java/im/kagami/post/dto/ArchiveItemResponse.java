package im.kagami.post.dto;

import im.kagami.post.domain.Post;
import im.kagami.post.domain.Tag;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * One row of the archive page: every published post in minimal form. Grouping by year/month is left
 * to the frontend, which knows the reader's locale.
 */
public record ArchiveItemResponse(
        Long id,
        String slug,
        String title,
        OffsetDateTime publishedAt,
        List<PostTagResponse> tags
) {

    public static ArchiveItemResponse from(Post post, List<Tag> tags) {
        return new ArchiveItemResponse(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getPublishedAt(),
                tags.stream().map(tag -> new PostTagResponse(tag.getId(), tag.getName())).toList());
    }
}
