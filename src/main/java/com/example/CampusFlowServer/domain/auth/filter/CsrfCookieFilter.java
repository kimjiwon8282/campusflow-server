package com.example.CampusFlowServer.domain.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.DeferredCsrfToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

public class CsrfCookieFilter extends OncePerRequestFilter {

    private static final String CSRF_ATTRIBUTE_NAME = "_csrf";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        CsrfToken csrfToken = resolveCsrfToken(request);
        if (csrfToken != null) {
            csrfToken.getToken();
        }

        filterChain.doFilter(request, response);
    }

    private CsrfToken resolveCsrfToken(HttpServletRequest request) {
        Object deferredCsrfToken = request.getAttribute(DeferredCsrfToken.class.getName());
        if (deferredCsrfToken instanceof DeferredCsrfToken token) {
            return token.get();
        }

        Object csrfToken = request.getAttribute(CsrfToken.class.getName());
        if (csrfToken instanceof CsrfToken token) {
            return token;
        }

        Object csrfAttribute = request.getAttribute(CSRF_ATTRIBUTE_NAME);
        if (csrfAttribute instanceof CsrfToken token) {
            return token;
        }

        return null;
    }
}
