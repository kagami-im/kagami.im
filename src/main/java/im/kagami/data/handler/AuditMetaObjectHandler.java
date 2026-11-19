package im.kagami.data.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import im.kagami.common.time.Clock;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * MyBatis-Plus auto-fill for created_at / updated_at on insert and update, reading the current
 * instant from {@link Clock}. Entity fields must be annotated with {@code @TableField(fill = ...)}.
 *
 * <p>Timestamps are owned by application code; the database triggers were dropped on purpose so
 * there is only one writer.
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    private final Clock clock;

    public AuditMetaObjectHandler(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        OffsetDateTime now = clock.now();
        strictInsertFill(metaObject, "createdAt", OffsetDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", OffsetDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", OffsetDateTime.class, clock.now());
    }
}
