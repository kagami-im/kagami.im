package im.kagami.post.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@TableName("posts")
@Data
public class Post {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PUBLISHED = 1;

    /** Database-assigned (bigserial). MyBatis-Plus' default snowflake ids overflow JS Number's safe
        integer range and get corrupted on the way through the frontend, so AUTO it is. */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** Author, references users.id. */
    private Long authorId;
    private String title;
    /** SEO-friendly URL segment; null means the post is addressed by id. */
    private String slug;
    /** Short excerpt for list pages; null falls back to whatever the frontend derives from content. */
    private String summary;
    private String coverUrl;
    /** Raw Markdown source; rendering is the frontend's job. */
    private String content;
    /** 0 = draft, 1 = published. */
    private Integer status;
    /** true = pinned to the top of the public list (archives unaffected). */
    private Boolean pinned;
    /** Approximate content length, recomputed on every write. */
    private Integer wordCount;
    /** Estimated reading time in minutes, recomputed on every write. */
    private Integer readingMinutes;
    /** Written on first publish; null while the post is a draft. */
    private OffsetDateTime publishedAt;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
