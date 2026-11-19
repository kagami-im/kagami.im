package im.kagami.post.dto;

import im.kagami.user.domain.User;

/**
 * Author card shown alongside posts on public pages. Deliberately narrow: no email, no bio, nothing
 * the reader does not need.
 */
public record PostAuthorResponse(
        Long id,
        String username,
        String nickname,
        String avatarUrl
) {

    public static PostAuthorResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new PostAuthorResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl());
    }
}
