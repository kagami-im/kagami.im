package im.kagami.security.ratelimit.backend;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;

/**
 * Seam between the rate-limiting policy layer (annotation + interceptor) and the bucket storage.
 *
 * <p>Every bucket4j bucket — a local, in-process one and a distributed one built over Redis —
 * exposes the same {@link Bucket} interface, so moving to a distributed backend only means adding
 * an implementation selected by {@code kagami.rate-limit.backend}; the policy code never changes.
 */
public interface BucketProvider {

    /**
     * Returns the bucket for {@code key}, creating it lazily with {@code configuration} when
     * missing. Implementations decide how long buckets live (in-memory eviction vs Redis TTL).
     */
    Bucket resolve(String key, BucketConfiguration configuration);
}
