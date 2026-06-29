package com.stop.api_gateway.filter;

import com.stop.api_gateway.http.HeaderStrippingHttpServletRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * İstemcinin X-User-Id / X-User-Role ile taklit yapmasını engeller.
 * Güvenilir değerler yalnızca {@link IdentityHeadersFilter} tarafından,
 * doğrulanmış JWT claim'lerinden sonra eklenir.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class StripClientIdentityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        filterChain.doFilter(
                HeaderStrippingHttpServletRequest.stripIdentityHeaders(request),
                response
        );
    }
}
