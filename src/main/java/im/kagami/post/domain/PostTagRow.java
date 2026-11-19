package im.kagami.post.domain;

import lombok.Data;

/**
 * Flat projection of post_tags joined with tags ({@code PostTagMapper.selectByPostIds}), so a whole
 * page of posts can fetch its tags in one query and get grouped by post id.
 */
@Data
public class PostTagRow {

    private Long postId;
    private Long tagId;
    private String tagName;
}
