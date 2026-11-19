package im.kagami.security.service;

import im.kagami.user.service.UserService;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads users by name; {@code DaoAuthenticationProvider} uses it to check the password. */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    /**
     * @throws UsernameNotFoundException when no such user exists; the authentication flow folds it
     *                                   into a generic credential error
     */
    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        im.kagami.user.domain.User user = userService.getUser(username);
        if (user == null) {
            throw new UsernameNotFoundException("No such user: " + username);
        }
        // No authorities: the codebase has no role model yet. The login flow reads the account
        // status (banned) off the entity afterwards.
        return User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .build();
    }
}
