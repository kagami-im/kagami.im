package im.kagami.user.dto;

import im.kagami.user.domain.User;

/**
 * Profile of the authenticated user ({@code GET /api/me}), as consumed by the console for display
 * and session state. Deliberately narrow: no password hash, and profile fields (email, bio, ...)
 * join when a feature actually surfaces them.
 */
public record UserMeResponse(
        Long id,
        String username,
        String nickname,
        String avatarUrl
) {

    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl());
    }
}
