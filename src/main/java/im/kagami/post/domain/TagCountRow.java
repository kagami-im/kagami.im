package im.kagami.post.domain;

import lombok.Data;

/**
 * Projection for the tag page ({@code TagMapper.selectTagCounts}): every tag plus the number of
 * published posts carrying it.
 */
@Data
public class TagCountRow {

    private Long tagId;
    private String name;
    private long postCount;
}
