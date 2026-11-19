package im.kagami.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import im.kagami.user.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * User persistence. Basic CRUD comes from MyBatis-Plus; the only addition is the lookup by name.
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** Looks a user up by name, password hash included. Returns null when absent. */
    User getByUsername(@Param("username") String username);
}
