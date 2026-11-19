package im.kagami.user.controller;

import im.kagami.common.security.CurrentUserProvider;
import im.kagami.user.dto.UserMeResponse;
import im.kagami.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    /** Requires authentication. */
    @GetMapping("/me")
    public UserMeResponse me() {
        return userService.getCurrentUser(currentUserProvider.currentUsername());
    }
}
