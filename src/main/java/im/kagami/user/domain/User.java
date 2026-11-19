package im.kagami.user.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Entity mapped to the {@code users} table. {@code passwordHash} holds a BCrypt digest and never
 * leaves the persistence layer.
 */
@TableName("users")
@Data
public class User {
    /** Database-assigned (bigserial); see Post.id for why not snowflake. */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** Login name, unique. */
    private String username;
    private String nickname;
    private String passwordHash;
    private String email;
    private String avatarUrl;
    private String bio;
    /** 0 = active, 1 = banned. */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
