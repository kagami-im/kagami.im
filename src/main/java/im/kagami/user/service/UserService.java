package im.kagami.user.service;

import im.kagami.common.exception.BusinessException;
import im.kagami.common.exception.ErrorCode;
import im.kagami.user.domain.User;
import im.kagami.user.dto.UserMeResponse;
import im.kagami.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    /**
     * Looks a user up by name, password hash included, for the login check and for the uniqueness
     * check on register. Returns null when there is no such user.
     */
    public User getUser(String username) {
        return userMapper.getByUsername(username);
    }

    /** Persists a new user; the caller supplies the password hash and the nickname. */
    public void register(User user) {
        userMapper.insert(user);
    }

    /** Single fetch for author cards on detail pages; null when missing. */
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    /** Batch fetch for building author cards on list pages; missing ids simply map nowhere. */
    public Map<Long, User> mapByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    /**
     * Profile of the authenticated user.
     *
     * @throws BusinessException {@code USER_NOT_FOUND} when the user no longer exists
     */
    public UserMeResponse getCurrentUser(String username) {
        return UserMeResponse.from(requireUser(username));
    }

    private User requireUser(String username) {
        User user = userMapper.getByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "No such user: " + username);
        }
        return user;
    }
}
