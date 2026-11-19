package im.kagami.post.controller;

import im.kagami.common.response.PageResponse;
import im.kagami.post.dto.ArchiveItemResponse;
import im.kagami.post.dto.PostDetailResponse;
import im.kagami.post.dto.PostSummaryResponse;
import im.kagami.post.dto.TagResponse;
import im.kagami.post.service.PostService;
import im.kagami.post.service.TagService;
import im.kagami.security.annotation.AnonymousAccess;
import im.kagami.security.ratelimit.annotation.RateLimit;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public, read-only post and tag API. Everything here is anonymous by design: any client can read
 * published content without a token.
 */
@RestController
@RequestMapping("/api")
@AnonymousAccess
@RateLimit(capacity = 120, duration = 1)
@Validated
@RequiredArgsConstructor
public class PostPublicController {

    private final PostService postService;
    private final TagService tagService;

    /** Published posts, newest first; filter by exact tag name. */
    @GetMapping("/posts")
    public PageResponse<PostSummaryResponse> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) long size,
            @RequestParam(required = false) String tag) {
        return postService.pagePublished(page, size, tag);
    }

    /** Literal path beats {@code /posts/{slugOrId}}, so this never gets captured as a slug. */
    @GetMapping("/posts/archives")
    public List<ArchiveItemResponse> archives() {
        return postService.listArchives();
    }

    /** Numeric segments resolve by id, anything else by slug. */
    @GetMapping("/posts/{slugOrId}")
    public PostDetailResponse detail(@PathVariable String slugOrId) {
        return postService.getPublished(slugOrId);
    }

    /** All tags with their published-post counts, busiest first. */
    @GetMapping("/tags")
    public List<TagResponse> tags() {
        return tagService.listTagCounts().stream()
                .map(TagResponse::from)
                .toList();
    }
}
