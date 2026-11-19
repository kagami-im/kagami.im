package im.kagami.common.exception;

import im.kagami.common.response.R;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Turns anything that escapes a controller into an {@link R} body. Business and validation failures
 * get dedicated handlers, as do the common "the request itself is wrong" cases (bad path/param/body,
 * wrong method) that would otherwise fall into the catch-all and read as a 500. Everything left
 * falls through to the catch-all, which answers 500 without leaking stack traces to the caller.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String DEFAULT_FIELD_MESSAGE = "Invalid value";

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<?>> handleOther(Exception e) {
        log.error("Request failed: ", e);
        return fail(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.SYSTEM_ERROR.getCode(),
                ErrorCode.SYSTEM_ERROR.getMessage(), null);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<?>> handleBusiness(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return fail(e.getHttpStatus(), e.getCode(), e.getMessage(), null);
    }

    /** Puts field-level errors into {@code data} so the frontend can point at the offending input. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<?>> handleValid(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? DEFAULT_FIELD_MESSAGE : fe.getDefaultMessage(),
                        (first, second) -> first));
        return fail(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.getCode(),
                ErrorCode.VALIDATION_ERROR.getMessage(), fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<?>> handleConstraintViolation(ConstraintViolationException e) {
        Map<String, String> violations = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> {
                            String path = v.getPropertyPath().toString();
                            int idx = path.lastIndexOf('.');
                            return idx >= 0 ? path.substring(idx + 1) : path;
                        },
                        v -> v.getMessage() == null ? DEFAULT_FIELD_MESSAGE : v.getMessage(),
                        (first, second) -> first));
        return fail(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.getCode(),
                ErrorCode.VALIDATION_ERROR.getMessage(), violations);
    }

    /** Malformed values in the request line: `?postId=abc` on a Long param. */
    @ExceptionHandler({ MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class })
    public ResponseEntity<R<?>> handleBadRequestLine(Exception e) {
        log.debug("Malformed request: {}", e.getMessage());
        return fail(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.getCode(),
                ErrorCode.VALIDATION_ERROR.getMessage(), null);
    }

    /** Unreadable body: invalid JSON or a type the payload cannot satisfy. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<?>> handleUnreadableBody(HttpMessageNotReadableException e) {
        log.debug("Unreadable request body: {}", e.getMessage());
        return fail(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.getCode(),
                ErrorCode.VALIDATION_ERROR.getMessage(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<?>> handleNoResource(NoResourceFoundException e) {
        log.debug("No handler for path: {}", e.getResourcePath());
        return fail(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                ErrorCode.RESOURCE_NOT_FOUND.getMessage(), null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.debug("Method not supported: {}", e.getMessage());
        return fail(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getCode(),
                ErrorCode.METHOD_NOT_ALLOWED.getMessage(), null);
    }

    private ResponseEntity<R<?>> fail(HttpStatus status, int code, String msg, Object data) {
        return ResponseEntity.status(status).body(R.fail(code, msg, data));
    }
}
