package im.kagami.security.service;

import im.kagami.common.exception.BusinessException;
import im.kagami.common.exception.ErrorCode;
import im.kagami.security.dto.LoginResponse;
import im.kagami.security.jwt.JwtProperties;
import im.kagami.security.jwt.JwtTokenProvider;
import im.kagami.user.domain.User;
import im.kagami.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    /**
     * @throws BusinessException {@code INVALID_CREDENTIALS} on bad credentials,
     *                           {@code FORBIDDEN} when the account is disabled
     */
    public LoginResponse login(String username, String password) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException e) {
            // Deliberately collapse "no such user", "wrong password" and "account locked" into one
            // error: it avoids account enumeration, and turning the Spring Security exception into a
            // business one keeps it out of the catch-all 500 handler.
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // The provider only proved the password; the account state lives on our entity, so one
        // lookup turns the authenticated name into the id the token carries.
        User user = userService.getUser(authentication.getName());
        if (user.getStatus() != null && user.getStatus() != 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Account is disabled");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), username);
        return new LoginResponse(token, jwtProperties.expirationIn());
    }

    /**
     * @throws BusinessException {@code USERNAME_EXISTS} when the name is already taken
     */
    public void register(String username, String password, String nickname) {
        // Check-then-insert races on concurrent registrations: the unique index gets the last word
        // and surfaces as a 500. Same known limitation as TagService.findOrCreateByNames.
        if (userService.getUser(username) != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "Username already taken: " + username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname == null || nickname.isBlank() ? username : nickname);
        user.setStatus(0);

        userService.register(user);
    }
}
