package com.stop.api_gateway.filter;

import com.stop.api_gateway.http.HeaderAddingHttpServletRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Yalnızca gateway tarafında doğrulanmış JWT'den türetilen kimlik bilgilerini downstream'e iletir.
 * İstemcinin gönderdiği X-User-* değerleri daha önce {@link StripClientIdentityHeadersFilter} ile düşürülür.
 */
public class IdentityHeadersFilter extends OncePerRequestFilter {

    public static final String X_USER_ID = "X-User-Id";
    public static final String X_USER_ROLE = "X-User-Role";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            var jwt = jwtAuth.getToken();
            HeaderAddingHttpServletRequest wrapped = new HeaderAddingHttpServletRequest(request);
            wrapped.addHeader(X_USER_ID, jwt.getSubject());
            String role = jwt.getClaimAsString("role");
            wrapped.addHeader(
                    X_USER_ROLE,
                    (role != null && !role.isBlank()) ? role : "USER"
            );
            filterChain.doFilter(wrapped, response);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
