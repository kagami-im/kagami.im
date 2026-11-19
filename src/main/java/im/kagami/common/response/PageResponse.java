package im.kagami.common.response;

import java.util.List;

/**
 * Uniform pagination envelope for list endpoints. Same role as {@link R}, but for page-shaped
 * payloads: the caller always receives the item list plus the paging metadata needed to render
 * pagination controls. Deliberately free of MyBatis-Plus types so the common layer stays
 * framework-neutral; services translate from {@code IPage} with {@link #of}.
 */
public record PageResponse<T>(List<T> items, long total, long page, long size) {

    public static <T> PageResponse<T> of(long page, long size, long total, List<T> items) {
        return new PageResponse<>(items, total, page, size);
    }
}
