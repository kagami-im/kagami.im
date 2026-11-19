package im.kagami.comment.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@TableName("comments")
@Data
public class Comment {

    public static final int STATUS_VISIBLE = 0;
    public static final int STATUS_HIDDEN = 1;

    /** Database-assigned (bigserial); see Post.id for why not snowflake. */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** Commented post, references posts.id. */
    private Long postId;
    /** Commenter, references users.id. */
    private Long authorId;
    /** NULL for top-level comments; replies point at a top-level comment of the same post. */
    private Long parentId;
    private String content;
    /** 0 = visible, 1 = hidden by the post author. */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
