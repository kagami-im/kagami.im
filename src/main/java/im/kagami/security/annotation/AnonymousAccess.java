package im.kagami.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller or handler method as reachable without authentication.
 *
 * <p>Endpoints annotated (at class or method level) with this are collected at startup by
 * {@code AnonymousUrlCollector} and exposed as {@code permitAll()} in the security filter chain,
 * so adding a public endpoint only requires this annotation — no edit to {@code SecurityConfig}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AnonymousAccess {
}
