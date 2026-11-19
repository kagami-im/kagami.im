package im.kagami.comment.controller;

import im.kagami.comment.dto.CommentThreadResponse;
import im.kagami.comment.service.CommentService;
import im.kagami.comment.domain.Comment;
import im.kagami.comment.dto.CreateCommentRequest;
import im.kagami.common.security.CurrentUserProvider;
import im.kagami.post.service.PostService;
import im.kagami.security.annotation.AnonymousAccess;
import im.kagami.security.ratelimit.annotation.RateLimit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Comment endpoints on the public API. Reading is anonymous; commenting requires a signed-in user.
 * Rate limited per client IP like every other public surface.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final PostService postService;
    private final CommentService commentService;
    private final CurrentUserProvider currentUserProvider;

    /** Visible two-level threads of a published post, oldest first. */
    @GetMapping("/posts/{slugOrId}/comments")
    @AnonymousAccess
    @RateLimit(capacity = 60, duration = 1)
    public List<CommentThreadResponse> threads(@PathVariable String slugOrId) {
        return commentService.listVisibleThreads(postService.requirePublishedPostId(slugOrId));
    }

    /** Creates a top-level comment or a reply; requires a signed-in user. */
    @PostMapping("/posts/{slugOrId}/comments")
    @RateLimit(capacity = 30, duration = 1)
    public CommentThreadResponse create(
            @PathVariable String slugOrId,
            @Valid @RequestBody CreateCommentRequest request) {
        Long postId = postService.requirePublishedPostId(slugOrId);
        Long authorId = currentUserProvider.currentUser().id();
        return commentService.create(postId, request.parentId(), authorId, request.content());
    }
}
