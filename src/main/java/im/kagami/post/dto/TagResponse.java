package im.kagami.post.dto;

import im.kagami.post.domain.TagCountRow;

/** A tag on the tag page: label plus how many published posts carry it. */
public record TagResponse(
        Long id,
        String name,
        long postCount
) {

    public static TagResponse from(TagCountRow row) {
        return new TagResponse(row.getTagId(), row.getName(), row.getPostCount());
    }
}
