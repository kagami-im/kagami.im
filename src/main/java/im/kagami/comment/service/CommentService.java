package im.kagami.comment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import im.kagami.comment.domain.Comment;
import im.kagami.comment.dto.CommentManageItemResponse;
import im.kagami.comment.dto.CommentThreadResponse;
import im.kagami.comment.mapper.CommentMapper;
import im.kagami.common.exception.BusinessException;
import im.kagami.common.exception.ErrorCode;
import im.kagami.common.time.Clock;
import im.kagami.post.dto.PostAuthorResponse;
import im.kagami.post.service.PostService;
import im.kagami.user.domain.User;
import im.kagami.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Two-level comment threads: a top-level comment plus flat replies to it. Replies may only target
 * top-level comments (enforced here, not in the schema), so no recursion exists anywhere. Hiding is
 * moderation by the post author; hidden top-level comments take their replies off the public view.
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final PostService postService;
    private final UserService userService;
    private final Clock clock;

    /** Visible threads of a published post, oldest first, replies attached to their top-level. */
    public List<CommentThreadResponse> listVisibleThreads(Long postId) {
        List<Comment> topLevel = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getStatus, Comment.STATUS_VISIBLE)
                .isNull(Comment::getParentId)
                .orderByAsc(Comment::getCreatedAt)
                .orderByAsc(Comment::getId));
        if (topLevel.isEmpty()) {
            return List.of();
        }

        List<Comment> replies = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .in(Comment::getParentId, topLevel.stream().map(Comment::getId).toList())
                .eq(Comment::getStatus, Comment.STATUS_VISIBLE)
                .orderByAsc(Comment::getCreatedAt)
                .orderByAsc(Comment::getId));
        Map<Long, User> authors = userService.mapByIds(authorIds(topLevel, replies));

        Map<Long, List<Comment>> repliesByParent = replies.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));
        return topLevel.stream()
                .map(top -> CommentThreadResponse.of(
                        top,
                        PostAuthorResponse.from(authors.get(top.getAuthorId())),
                        repliesByParent.getOrDefault(top.getId(), List.of()).stream()
                                .map(reply -> CommentThreadResponse.of(
                                        reply,
                                        PostAuthorResponse.from(authors.get(reply.getAuthorId())),
                                        List.of()))
                                .toList()))
                .toList();
    }

    /**
     * Creates a top-level comment or a reply. The post must be published; a parentId must point at
     * a visible top-level comment of the same post (replies to replies are rejected). The created
     * comment comes back in its public shape (no moderation fields).
     */
    @Transactional
    public CommentThreadResponse create(Long postId, Long parentId, Long authorId, String content) {
        if (parentId != null) {
            Comment parent = requireComment(parentId);
            if (!parent.getPostId().equals(postId) || parent.getStatus() != Comment.STATUS_VISIBLE) {
                throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND,
                        "No such comment to reply to: " + parentId);
            }
            if (parent.getParentId() != null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Replies can only target top-level comments");
            }
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(authorId);
        comment.setParentId(parentId);
        comment.setContent(content.trim());
        comment.setStatus(Comment.STATUS_VISIBLE);
        commentMapper.insert(comment);
        return CommentThreadResponse.of(comment,
                PostAuthorResponse.from(userService.getById(authorId)), List.of());
    }

    /** Moderation list for a post's author: every comment, hidden ones included. */
    public List<CommentManageItemResponse> listForModeration(Long postId, Long operatorId) {
        postService.requireOwnedPost(postId, operatorId);
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .orderByAsc(Comment::getCreatedAt)
                .orderByAsc(Comment::getId));
        Map<Long, User> authors = userService.mapByIds(comments.stream().map(Comment::getAuthorId).toList());
        return comments.stream()
                .map(comment -> CommentManageItemResponse.from(comment,
                        PostAuthorResponse.from(authors.get(comment.getAuthorId()))))
                .toList();
    }

    /** Hide/unhide a comment; moderation is reserved to the post author. */
    @Transactional
    public void setStatus(Long commentId, int status, Long operatorId) {
        Comment comment = requireComment(commentId);
        postService.requireOwnedPost(comment.getPostId(), operatorId);
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                .eq(Comment::getId, commentId)
                .set(Comment::getStatus, status)
                .set(Comment::getUpdatedAt, clock.now()));
    }

    /**
     * Deletes a comment. Both the comment's own author and the author of the commented post may
     * delete; deleting a top-level comment takes its replies with it (FK cascade).
     */
    @Transactional
    public void delete(Long commentId, Long operatorId) {
        Comment comment = requireComment(commentId);
        if (!comment.getAuthorId().equals(operatorId)) {
            postService.requireOwnedPost(comment.getPostId(), operatorId);
        }
        commentMapper.deleteById(commentId);
    }

    // ---------- helpers ----------

    private Comment requireComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "No such comment: " + commentId);
        }
        return comment;
    }

    private static Collection<Long> authorIds(List<Comment> topLevel, List<Comment> replies) {
        Collection<Long> ids = new HashSet<>();
        topLevel.forEach(comment -> ids.add(comment.getAuthorId()));
        replies.forEach(comment -> ids.add(comment.getAuthorId()));
        return ids;
    }
}
