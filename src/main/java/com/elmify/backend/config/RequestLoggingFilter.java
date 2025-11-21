package com.elmify.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Debug filter to log all requests and their authentication state.
 * Only enabled when elmify.debug.request-logging=true (disabled by default).
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
@ConditionalOnProperty(name = "elmify.debug.request-logging", havingValue = "true", matchIfMissing = false)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        log.debug("REQUEST: {} {} | Auth: {} | Principal: {} | Authorities: {} | HasToken: {}",
            method,
            uri,
            auth != null ? auth.getClass().getSimpleName() : "null",
            auth != null ? auth.getPrincipal() : "null",
            auth != null ? auth.getAuthorities() : "null",
            authHeader != null ? "yes" : "no"
        );

        try {
            filterChain.doFilter(request, response);
        } finally {
            log.debug("RESPONSE: {} {} | Status: {}", method, uri, response.getStatus());
        }
    }
}
