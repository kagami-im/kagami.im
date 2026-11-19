package im.kagami.security.jwt;

import im.kagami.common.exception.ErrorCode;
import im.kagami.common.response.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Unauthenticated entry point: a protected resource was requested without a usable JWT, so answer
 * with 401 and an {@code R(code=UNAUTHORIZED)} body instead of the default redirect to a login page.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus().value());
        objectMapper.writeValue(response.getWriter(),
                R.fail(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage(), null));
    }
}
