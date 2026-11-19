package im.kagami.common.response;

/**
 * Uniform API response envelope. Bare controller return values are wrapped into this structure
 * by {@code ResultWrapperAdvice}.
 *
 * @param <T>  payload type
 * @param code business code: 200 on success, otherwise a value from {@link ErrorCode}
 * @param msg  human-readable status description
 * @param data payload, may be null
 */
public record R<T>(int code, String msg, T data) {

    /** Success code, kept in sync with the frontend SUCCESS_CODE constant. */
    public static final int SUCCESS = 200;
    public static final String SUCCESS_MSG = "success";

    public static <T> R<T> success(T data) {
        return new R<>(SUCCESS, SUCCESS_MSG, data);
    }

    public static <T> R<T> success() {
        return success(null);
    }

    /** Failure response; {@code data} may hold a field-name to message map. */
    public static <T> R<T> fail(int code, String msg, T data) {
        return new R<>(code, msg, data);
    }
}
