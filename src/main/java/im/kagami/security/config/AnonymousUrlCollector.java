package im.kagami.security.config;

import im.kagami.security.annotation.AnonymousAccess;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.util.stream.Stream;

/**
 * Collects every request-mapping path annotated (at class or method level) with
 * {@link AnonymousAccess}, so the security filter chain can expose them as {@code permitAll()}
 * without a hardcoded whitelist.
 *
 * <p>Handler methods are only fully registered after the context refreshes, so the scan runs in a
 * {@link PostConstruct} hook once this bean and the {@link RequestMappingHandlerMapping} are ready.
 */
@Configuration
@RequiredArgsConstructor
public class AnonymousUrlCollector {

    private final RequestMappingHandlerMapping handlerMapping;

    @Getter
    private String[] anonymousUrls = new String[0];

    @PostConstruct
    public void collect() {
        anonymousUrls = handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> isAnonymous(entry.getValue()))
                .flatMap(entry -> patternsOf(entry.getKey()))
                .distinct()
                .toArray(String[]::new);
    }

    private boolean isAnonymous(HandlerMethod method) {
        return AnnotatedElementUtils.isAnnotated(method.getBeanType(), AnonymousAccess.class)
                || method.hasMethodAnnotation(AnonymousAccess.class);
    }

    private Stream<String> patternsOf(RequestMappingInfo info) {
        var condition = info.getPathPatternsCondition();
        return condition == null ? Stream.empty()
                : condition.getPatterns().stream().map(PathPattern::getPatternString);
    }
}
