package im.kagami.security.ratelimit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Declares a rate limit for a controller class or handler method. A method-level annotation
 * overrides the class-level one. Requests are counted per client IP and per route, regardless
 * of their outcome: a failed login consumes budget just like a successful one.
 *
 * <p>Semantics are a token bucket of {@link #capacity()} tokens refilled greedily at
 * {@code capacity} per {@link #duration()}: a client may spend the whole budget in one burst,
 * after which requests are throttled until tokens regenerate. Exceeding the limit is answered
 * with 429, an {@code R} body and a {@code Retry-After} header (see {@code RateLimitInterceptor}).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** Maximum number of requests per refill period, i.e. the bucket size. */
    int capacity();

    /** Length of the refill period; tokens regenerate continuously at {@code capacity} per period. */
    long duration();

    /** Time unit of {@link #duration()}. */
    TimeUnit unit() default TimeUnit.MINUTES;
}
