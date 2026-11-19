package im.kagami.security.ratelimit;

import tools.jackson.databind.ObjectMapper;
import im.kagami.security.ratelimit.backend.BucketProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC wiring for rate limiting: the interceptor covers every /api route; endpoints without a
 * {@code @RateLimit} declaration pass through untouched.
 */
@Configuration
@RequiredArgsConstructor
public class RateLimitConfig implements WebMvcConfigurer {

    private final BucketProvider bucketProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(bucketProvider, objectMapper))
                .addPathPatterns("/api/**");
    }
}
