package im.kagami.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a business rule rejects the request. {@code GlobalExceptionHandler} turns it into
 * an {@code R(code, msg)} body plus the matching HTTP status.
 *
 * <p>Unlike an unhandled system exception, this one carries an explicit {@link ErrorCode},
 * which lets the frontend route errors by business code instead of by HTTP status alone.
 */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    private final HttpStatus httpStatus;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }
}
