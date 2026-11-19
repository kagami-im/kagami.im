package im.kagami.security.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import im.kagami.common.security.CurrentUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * Runs once per request: reads the bearer token from the configured header and, when it verifies,
 * puts the identity into the SecurityContext.
 *
 * <p>A failed parse only clears the context and lets the chain continue. Whether the request ends
 * up as 401 is decided by the authorization rules in {@code SecurityConfig} and by
 * {@link JwtAuthenticationEntryPoint}, so this filter never aborts a request on its own.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(jwtProperties.header());
        if (header != null && header.startsWith(jwtProperties.tokenPrefix())) {
            String token = header.substring(jwtProperties.tokenPrefix().length());
            try {
                SecurityContextHolder.getContext().setAuthentication(buildAuthentication(token, request));
            } catch (IllegalArgumentException | ParseException e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(String token, HttpServletRequest request)
            throws ParseException {
        JWTClaimsSet claims = tokenProvider.parseAndValidate(token);
        Long userId = claims.getLongClaim(JwtTokenProvider.CLAIM_USER_ID);

        // A lightweight principal keeps later identity lookups off the database. No granted
        // authorities: nothing in the codebase branches on roles yet.
        CurrentUser principal = new CurrentUser(userId, claims.getSubject());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authentication;
    }
}
