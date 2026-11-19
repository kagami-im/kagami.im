package im.kagami.security.ratelimit;

import tools.jackson.databind.ObjectMapper;
import im.kagami.common.exception.ErrorCode;
import im.kagami.common.response.R;
import im.kagami.security.ratelimit.annotation.RateLimit;
import im.kagami.security.ratelimit.backend.BucketProvider;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Enforces {@link RateLimit} declarations, method-level winning over class-level. Runs in the
 * MVC layer, i.e. after the security filter chain — anonymous endpoints reach it just fine
 * because security lets them through.
 */
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String KEY_PREFIX = "rate-limit:";
    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private final BucketProvider bucketProvider;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RateLimit methodLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        RateLimit limit = methodLimit != null
                ? methodLimit
                : handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        if (limit == null) {
            return true;
        }

        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(bandwidth -> bandwidth
                        .capacity(limit.capacity())
                        .refillGreedy(limit.capacity(), Duration.of(limit.duration(), limit.unit().toChronoUnit())))
                .build();
        ConsumptionProbe probe = bucketProvider.resolve(key(request), configuration)
                .tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return true;
        }

        reject(response, probe);
        return false;
    }

    private String key(HttpServletRequest request) {
        // The matching pattern (e.g. /api/users/{id}) keeps every call of a templated route in one
        // bucket; the plain URI would fork a bucket per id.
        String route = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (route == null) {
            route = request.getRequestURI();
        }
        return KEY_PREFIX + route + ":" + clientIp(request);
    }

    /**
     * Client IP as seen by the application. Behind a reverse proxy the socket address is the
     * proxy's, so the first X-Forwarded-For entry is taken; the deployment must guarantee that
     * the proxy overwrites (rather than appends to) this header, otherwise clients can spoof it.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Answers 429 directly instead of throwing: the {@code Retry-After} value comes from the probe. */
    private void reject(HttpServletResponse response, ConsumptionProbe probe) throws IOException {
        ErrorCode errorCode = ErrorCode.TOO_MANY_REQUESTS;
        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                R.fail(errorCode.getCode(), errorCode.getMessage(), null)));
    }
}
