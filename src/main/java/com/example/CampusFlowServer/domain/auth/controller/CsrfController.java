package com.example.CampusFlowServer.domain.auth.controller;

import com.example.CampusFlowServer.domain.auth.properties.CookieProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/csrf")
@RequiredArgsConstructor
public class CsrfController {

    private static final String CSRF_ATTRIBUTE_NAME = "_csrf";

    private final CookieProperties cookieProperties;

    @GetMapping
    public Map<String, String> issueCsrfToken(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        CsrfToken csrfToken = resolveCsrfToken(request);
        if (csrfToken != null) {
            response.addHeader(
                HttpHeaders.SET_COOKIE,
                ResponseCookie.from(cookieProperties.getCsrfTokenName(), csrfToken.getToken())
                    .httpOnly(false)
                    .secure(cookieProperties.isSecure())
                    .sameSite(cookieProperties.getSameSite())
                    .path("/")
                    .build()
                    .toString()
            );
        }
        return Map.of("message", "CSRF token issued");
    }

    private CsrfToken resolveCsrfToken(HttpServletRequest request) {
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
