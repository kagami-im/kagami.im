package im.kagami.security.ratelimit.backend;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.local.LocalBucketBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Local {@link BucketProvider}: buckets live in a Caffeine cache, bounded in size and evicted
 * after an idle period so abandoned keys (client IPs that stopped calling) cannot grow the heap.
 *
 * <p>State is process-local: it is lost on restart, and multiple instances would count
 * independently. The Redis backend changes exactly that, without touching the callers.
 */
@Component
@ConditionalOnProperty(name = "kagami.rate-limit.backend", havingValue = "local", matchIfMissing = true)
public class LocalBucketProvider implements BucketProvider {

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    @Override
    public Bucket resolve(String key, BucketConfiguration configuration) {
        return buckets.get(key, _ -> {
            // Rebuild from the configuration's bandwidths: the same BucketConfiguration is what
            // the future Redis backend will consume, keeping both providers symmetric.
            LocalBucketBuilder builder = Bucket.builder();
            for (Bandwidth bandwidth : configuration.getBandwidths()) {
                builder.addLimit(bandwidth);
            }
            return builder.build();
        });
    }
}
