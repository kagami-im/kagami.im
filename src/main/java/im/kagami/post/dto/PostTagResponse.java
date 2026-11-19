package im.kagami.post.dto;

import im.kagami.post.domain.Tag;

/** Tag attached to a post, as rendered inside post responses. */
public record PostTagResponse(
        Long id,
        String name
) {

    public static PostTagResponse from(Tag tag) {
        return new PostTagResponse(tag.getId(), tag.getName());
    }
}
