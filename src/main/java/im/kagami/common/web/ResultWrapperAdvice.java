package im.kagami.common.web;

import im.kagami.common.response.R;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.ObjectMapper;

/**
 * Wraps bare controller return values into {@link R} after the handler runs and before
 * serialization.
 *
 * <p>Values that are already complete pass straight through: {@code R} and {@code ResponseEntity}.
 * {@code null} becomes {@code R.success()}. {@code String} has to be serialized by hand, because
 * Spring gives it to StringHttpMessageConverter rather than to Jackson.
 */
@RestControllerAdvice
public class ResultWrapperAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public ResultWrapperAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public @Nullable Object beforeBodyWrite(@Nullable Object body,
                                            MethodParameter returnType,
                                            MediaType selectedContentType,
                                            Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                            ServerHttpRequest request,
                                            ServerHttpResponse response) {
        if (body instanceof R<?> || body instanceof ResponseEntity<?>) {
            return body;
        }
        if (body == null) {
            return R.success();
        }
        if (body instanceof String str) {
            return objectMapper.writeValueAsString(R.success(str));
        }
        return R.success(body);
    }
}
