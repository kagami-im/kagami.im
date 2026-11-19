package im.kagami.comment.controller;

import im.kagami.comment.domain.Comment;
import im.kagami.comment.dto.CommentManageItemResponse;
import im.kagami.comment.service.CommentService;
import im.kagami.common.security.CurrentUserProvider;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Moderation endpoints for the author of a post: list every comment on it (hidden included),
 * hide/unhide, delete. All of them require a JWT and re-check ownership in the service.
 */
@RestController
@RequestMapping("/api/manage/comments")
@Validated
@RequiredArgsConstructor
public class CommentManageController {

    private final CommentService commentService;
    private final CurrentUserProvider currentUserProvider;

    /** Every comment on one of the author's posts, hidden included, oldest first. */
    @GetMapping
    public List<CommentManageItemResponse> list(@RequestParam @Min(1) Long postId) {
        return commentService.listForModeration(postId, currentUserProvider.currentUser().id());
    }

    @PostMapping("/{id}/hide")
    public void hide(@PathVariable @Min(1) Long id) {
        commentService.setStatus(id, Comment.STATUS_HIDDEN, currentUserProvider.currentUser().id());
    }

    @PostMapping("/{id}/unhide")
    public void unhide(@PathVariable @Min(1) Long id) {
        commentService.setStatus(id, Comment.STATUS_VISIBLE, currentUserProvider.currentUser().id());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable @Min(1) Long id) {
        commentService.delete(id, currentUserProvider.currentUser().id());
    }
}
