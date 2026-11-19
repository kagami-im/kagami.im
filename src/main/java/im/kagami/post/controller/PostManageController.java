package im.kagami.post.controller;

import im.kagami.common.response.PageResponse;
import im.kagami.post.dto.CreatePostRequest;
import im.kagami.post.dto.PostManageDetailResponse;
import im.kagami.post.dto.PostManageItemResponse;
import im.kagami.post.dto.UpdatePostRequest;
import im.kagami.post.service.PostService;
import im.kagami.common.security.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Author-side post management. Every route requires a JWT (no {@code @AnonymousAccess}) and only ever
 * sees posts owned by the signed-in author; the author id comes from the token, never from the client.
 */
@RestController
@RequestMapping("/api/manage/posts")
@Validated
@RequiredArgsConstructor
public class PostManageController {

    private final PostService postService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public PageResponse<PostManageItemResponse> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) long size,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status,
            @RequestParam(required = false) @Size(max = 50) String keyword) {
        return postService.pageMine(
                page, size, status, keyword, currentUserProvider.currentUser().id());
    }

    @GetMapping("/{id}")
    public PostManageDetailResponse detail(@PathVariable @Min(1) Long id) {
        return postService.getMine(id, currentUserProvider.currentUser().id());
    }

    /** Creates a draft; publishing is a separate, explicit step. */
    @PostMapping
    public PostManageDetailResponse create(@Valid @RequestBody CreatePostRequest request) {
        return postService.create(currentUserProvider.currentUser().id(), request);
    }

    @PutMapping("/{id}")
    public PostManageDetailResponse update(
            @PathVariable @Min(1) Long id, @Valid @RequestBody UpdatePostRequest request) {
        return postService.update(id, currentUserProvider.currentUser().id(), request);
    }

    @PostMapping("/{id}/publish")
    public void publish(@PathVariable @Min(1) Long id) {
        postService.publish(id, currentUserProvider.currentUser().id());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable @Min(1) Long id) {
        postService.delete(id, currentUserProvider.currentUser().id());
    }
}
