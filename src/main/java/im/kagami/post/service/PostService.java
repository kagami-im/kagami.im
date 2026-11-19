package im.kagami.post.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import im.kagami.common.exception.BusinessException;
import im.kagami.common.exception.ErrorCode;
import im.kagami.common.response.PageResponse;
import im.kagami.common.time.Clock;
import im.kagami.post.domain.Post;
import im.kagami.post.domain.Tag;
import im.kagami.post.dto.ArchiveItemResponse;
import im.kagami.post.dto.CreatePostRequest;
import im.kagami.post.dto.PostAuthorResponse;
import im.kagami.post.dto.PostDetailResponse;
import im.kagami.post.dto.PostManageDetailResponse;
import im.kagami.post.dto.PostManageItemResponse;
import im.kagami.post.dto.PostSummaryResponse;
import im.kagami.post.dto.UpdatePostRequest;
import im.kagami.post.mapper.PostMapper;
import im.kagami.user.domain.User;
import im.kagami.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Post use cases for both audiences: the public read API (published posts only) and the author's
 * management API (own posts, any status). Authorship is enforced here, not in the controllers.
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private static final Pattern LATIN_WORD = Pattern.compile("[A-Za-z0-9]+");

    private final PostMapper postMapper;
    private final TagService tagService;
    private final UserService userService;
    private final Clock clock;

    // ---------- public reads ----------

    /** Published posts, newest first, optionally filtered by tag name. */
    public PageResponse<PostSummaryResponse> pagePublished(long page, long size, String tagName) {
        Page<Post> result = postMapper.selectPublishedPage(new Page<>(page, size), tagName);
        List<Post> posts = result.getRecords();
        Map<Long, User> authors = userService.mapByIds(authorIds(posts));
        Map<Long, List<Tag>> tags = tagService.tagsByPostIds(postIds(posts));
        List<PostSummaryResponse> items = posts.stream()
                .map(post -> PostSummaryResponse.from(post,
                        PostAuthorResponse.from(authors.get(post.getAuthorId())),
                        tags.getOrDefault(post.getId(), List.of())))
                .toList();
        return PageResponse.of(result.getCurrent(), result.getSize(), result.getTotal(), items);
    }

    /** Resolves a public slug-or-id path segment to a published post's id; 404 when missing. */
    public Long requirePublishedPostId(String slugOrId) {
        return findPublishedOrThrow(slugOrId).getId();
    }

    /** One published post by id or slug, for the public detail page. */
    public PostDetailResponse getPublished(String slugOrId) {
        Post post = findPublishedOrThrow(slugOrId);
        return PostDetailResponse.from(post,
                PostAuthorResponse.from(userService.getById(post.getAuthorId())), tagsOf(post.getId()));
    }

    /** Every published post in minimal form, newest first; the archive page groups it by month. */
    public List<ArchiveItemResponse> listArchives() {
        List<Post> posts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .select(Post::getId, Post::getSlug, Post::getTitle, Post::getPublishedAt)
                .eq(Post::getStatus, Post.STATUS_PUBLISHED)
                .orderByDesc(Post::getPublishedAt)
                .orderByDesc(Post::getId));
        Map<Long, List<Tag>> tags = tagService.tagsByPostIds(postIds(posts));
        return posts.stream()
                .map(post -> ArchiveItemResponse.from(post, tags.getOrDefault(post.getId(), List.of())))
                .toList();
    }

    // ---------- author management ----------

    /** The author's own posts, any status, newest change first; optional status filter and title search. */
    public PageResponse<PostManageItemResponse> pageMine(
            long page, long size, Integer status, String keyword, long authorId) {
        // The wrapper's condition flag only controls the SQL fragment; arguments are evaluated
        // eagerly, so the keyword must be normalized before it reaches `.like(...)`.
        String search = keyword == null || keyword.isBlank() ? null : keyword.trim();
        // List rows never carry the body: project every column except content.
        Page<Post> result = postMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Post>()
                .select(Post.class, field -> !"content".equals(field.getColumn()))
                .eq(Post::getAuthorId, authorId)
                .eq(status != null, Post::getStatus, status)
                .like(search != null, Post::getTitle, escapeLike(search))
                .orderByDesc(Post::getUpdatedAt));
        List<Post> posts = result.getRecords();
        Map<Long, List<Tag>> tags = tagService.tagsByPostIds(postIds(posts));
        List<PostManageItemResponse> items = posts.stream()
                .map(post -> PostManageItemResponse.from(post, tags.getOrDefault(post.getId(), List.of())))
                .toList();
        return PageResponse.of(result.getCurrent(), result.getSize(), result.getTotal(), items);
    }

    /**
     * Full post for the editor, any status, owner only.
     *
     * @throws BusinessException {@code POST_NOT_FOUND} / {@code FORBIDDEN} when not the owner's post
     */
    public PostManageDetailResponse getMine(Long id, long authorId) {
        return PostManageDetailResponse.from(requireOwnedPost(id, authorId), tagsOf(id));
    }

    /**
     * Creates a draft. The payload cannot set a status: publishing is an explicit endpoint, so a
     * misclick while writing never makes a post public.
     */
    @Transactional
    public PostManageDetailResponse create(long authorId, CreatePostRequest request) {
        String slug = normalizeSlug(request.slug());
        requireSlugFree(slug, null);
        Post post = new Post();
        post.setAuthorId(authorId);
        post.setTitle(request.title().trim());
        post.setSlug(slug);
        post.setSummary(blankToNull(request.summary()));
        post.setCoverUrl(blankToNull(request.coverUrl()));
        post.setContent(request.content());
        post.setStatus(Post.STATUS_DRAFT);
        post.setPinned(Boolean.TRUE.equals(request.pinned()));
        ReadingStats stats = computeReadingStats(request.content());
        post.setWordCount(stats.wordCount());
        post.setReadingMinutes(stats.minutes());
        post.setPublishedAt(null);
        postMapper.insert(post);
        tagService.replacePostTags(post.getId(), request.tags());
        return PostManageDetailResponse.from(post, tagsOf(post.getId()));
    }

    /**
     * Rewrites the editable fields; the publish state is deliberately untouched.
     *
     * @throws BusinessException {@code POST_SLUG_EXISTS} when another post already owns the slug
     */
    @Transactional
    public PostManageDetailResponse update(Long id, long authorId, UpdatePostRequest request) {
        requireOwnedPost(id, authorId);
        String slug = normalizeSlug(request.slug());
        requireSlugFree(slug, id);
        ReadingStats stats = computeReadingStats(request.content());
        // Wrapper-only update on purpose: updateById skips null fields (default field strategy), so
        // clearing summary/slug/coverUrl would silently do nothing. The MetaObjectHandler does not
        // fire for wrapper-only updates either, hence the explicit updatedAt set.
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, id)
                .set(Post::getTitle, request.title().trim())
                .set(Post::getSlug, slug)
                .set(Post::getSummary, blankToNull(request.summary()))
                .set(Post::getCoverUrl, blankToNull(request.coverUrl()))
                .set(Post::getContent, request.content())
                .set(Post::getPinned, Boolean.TRUE.equals(request.pinned()))
                .set(Post::getWordCount, stats.wordCount())
                .set(Post::getReadingMinutes, stats.minutes())
                .set(Post::getUpdatedAt, clock.now()));
        tagService.replacePostTags(id, request.tags());
        return PostManageDetailResponse.from(postMapper.selectById(id), tagsOf(id));
    }

    /**
     * Draft to published; {@code published_at} is stamped once. Publishing an already published post
     * is a no-op so the endpoint can be retried safely.
     */
    @Transactional
    public void publish(Long id, long authorId) {
        Post post = requireOwnedPost(id, authorId);
        if (post.getStatus() == Post.STATUS_PUBLISHED) {
            return;
        }
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, id)
                .set(Post::getStatus, Post.STATUS_PUBLISHED)
                .set(Post::getPublishedAt, clock.now())
                .set(Post::getUpdatedAt, clock.now()));
    }

    /**
     * @throws BusinessException {@code POST_NOT_FOUND} / {@code FORBIDDEN} when not the owner's post
     */
    @Transactional
    public void delete(Long id, long authorId) {
        requireOwnedPost(id, authorId);
        postMapper.deleteById(id); // link rows in post_tags go with the FK cascade
    }

    /**
     * Asserts the post exists and belongs to the operator; the ownership gate shared by the
     * management API and comment moderation.
     *
     * @return the post, for callers that continue working with it
     */
    public Post requireOwnedPost(Long id, Long authorId) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND, "No such post: " + id);
        }
        if (!post.getAuthorId().equals(authorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "You are not the author of this post");
        }
        return post;
    }

    // ---------- helpers ----------

    private Post findPublishedOrThrow(String slugOrId) {
        Post post = findPublished(slugOrId);
        if (post == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND, "No such published post: " + slugOrId);
        }
        return post;
    }

    /** Numeric segments address by id (capped at 18 digits so it always fits a long), the rest by slug. */
    private Post findPublished(String slugOrId) {
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, Post.STATUS_PUBLISHED);
        if (slugOrId.matches("\\d{1,18}")) {
            return postMapper.selectOne(query.eq(Post::getId, Long.parseLong(slugOrId)));
        }
        return postMapper.selectOne(query.eq(Post::getSlug, slugOrId));
    }

    /** Slugs live in the same URL namespace as ids, so pure digits are reserved. */
    private String normalizeSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        String trimmed = slug.trim();
        if (trimmed.matches("\\d+")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Slug cannot be all digits: /api/posts/<digits> resolves by post id");
        }
        return trimmed;
    }

    /** Escapes LIKE wildcards so a user-typed `100%` searches for "100%" and not "100" + anything. */
    private static String escapeLike(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * Rough reading metadata, recomputed on every write. CJK characters count as one unit each and
     * consecutive latin/digit runs as one word; ~400 units per minute is a common rule of thumb for
     * mixed CJK reading speed. Deliberately approximate — code blocks and Markdown syntax included.
     */
    private static ReadingStats computeReadingStats(String content) {
        int cjk = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                cjk++;
            }
        }
        Matcher latinWord = LATIN_WORD.matcher(content);
        int latinWords = 0;
        while (latinWord.find()) {
            latinWords++;
        }
        int wordCount = cjk + latinWords;
        return new ReadingStats(wordCount, Math.max(1, Math.round(wordCount / 400f)));
    }

    private record ReadingStats(int wordCount, int minutes) {
    }

    private void requireSlugFree(String slug, Long excludeId) {
        if (slug == null) {
            return;
        }
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<Post>().eq(Post::getSlug, slug);
        if (excludeId != null) {
            query.ne(Post::getId, excludeId);
        }
        if (postMapper.exists(query)) {
            throw new BusinessException(ErrorCode.POST_SLUG_EXISTS, "Slug already in use: " + slug);
        }
    }

    private List<Tag> tagsOf(Long postId) {
        return tagService.tagsByPostIds(List.of(postId)).getOrDefault(postId, List.of());
    }

    private static List<Long> authorIds(List<Post> posts) {
        return posts.stream().map(Post::getAuthorId).distinct().toList();
    }

    private static List<Long> postIds(List<Post> posts) {
        return posts.stream().map(Post::getId).toList();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
