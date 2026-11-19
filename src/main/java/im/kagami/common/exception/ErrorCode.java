package im.kagami.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Central catalogue of business error codes. Two dimensions are kept apart:
 * <ul>
 *     <li>httpStatus: the HTTP status to respond with;</li>
 *     <li>code: the 5-digit business code reported as {@code R.code}, whose leading digit
 *         identifies the domain so the frontend can branch on it.</li>
 * </ul>
 * Ranges: 1xxxx user/auth, 2xxxx content/posts, 9xxxx common/system; 3xxxx-8xxxx are reserved
 * for future domains.
 * Success uses {@code R.SUCCESS} (200) and consumes no business code range.
 */
@Getter
public enum ErrorCode {
    SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 90001, "System error"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, 90002, "Request validation failed"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, 90003, "Too many requests"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, 90004, "Resource not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, 90005, "Method not allowed"),

    POST_NOT_FOUND(HttpStatus.NOT_FOUND, 20001, "Post not found"),
    POST_SLUG_EXISTS(HttpStatus.CONFLICT, 20002, "Slug already in use"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, 21001, "Comment not found"),

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, 10001, "Incorrect username or password"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 10002, "Not authenticated"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 10003, "User not found"),
    USERNAME_EXISTS(HttpStatus.CONFLICT, 10004, "Username already taken"),
    FORBIDDEN(HttpStatus.FORBIDDEN, 10007, "Forbidden");

    private final HttpStatus httpStatus;
    private final int code;
    /** Default message; callers may override it through {@link BusinessException}. */
    private final String message;

    ErrorCode(HttpStatus httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
