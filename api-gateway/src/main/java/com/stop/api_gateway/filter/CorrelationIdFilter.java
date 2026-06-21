package com.stop.api_gateway.filter;

import com.stop.api_gateway.http.HeaderAddingHttpServletRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String id = UUID.randomUUID().toString();

        HeaderAddingHttpServletRequest wrapped = new HeaderAddingHttpServletRequest(request);
        wrapped.addHeader(HEADER, id);
        response.setHeader(HEADER, id);
        MDC.put("correlationId", id);
        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
