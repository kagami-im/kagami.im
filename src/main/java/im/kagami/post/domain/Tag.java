package im.kagami.post.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@TableName("tags")
@Data
public class Tag {

    /** Database-assigned (bigserial); see Post.id for why not snowflake. */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** Unique, case-sensitive label, e.g. "Java" and "java" are two tags. */
    private String name;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
