package im.kagami.common.security;

import im.kagami.common.exception.BusinessException;
import im.kagami.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Single place to read the authenticated identity, so no other class touches SecurityContext.
 * The principal is always a {@link CurrentUser}, written by {@code JwtAuthenticationFilter}.
 */
@Component
public class CurrentUserProvider {

    public String currentUsername() {
        return currentUser().username();
    }

    /**
     * @throws BusinessException {@code UNAUTHORIZED} when the request is not authenticated
     */
    public CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser;
    }
}
