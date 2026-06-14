package com.example.CampusFlowServer.domain.auth.cookie;

import com.example.CampusFlowServer.domain.auth.dto.TokenPair;
import com.example.CampusFlowServer.domain.auth.properties.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthCookieManager {

    private static final String COOKIE_PATH = "/";

    private final CookieProperties cookieProperties;

    public void addTokenCookies(HttpServletResponse response, TokenPair tokenPair) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            createTokenCookie(
                cookieProperties.getAccessTokenName(),
                tokenPair.accessToken(),
                cookieProperties.getAccessTokenMaxAgeSeconds()
            ).toString()
        );
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            createTokenCookie(
                cookieProperties.getRefreshTokenName(),
                tokenPair.refreshToken(),
                cookieProperties.getRefreshTokenMaxAgeSeconds()
            ).toString()
        );
    }

    public String extractAccessToken(HttpServletRequest request) {
        return extractCookieValue(request, cookieProperties.getAccessTokenName());
    }

    public String extractRefreshToken(HttpServletRequest request) {
        return extractCookieValue(request, cookieProperties.getRefreshTokenName());
    }

    public void expireTokenCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, createExpiredAccessTokenCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createExpiredRefreshTokenCookie().toString());
    }

    public ResponseCookie createExpiredAccessTokenCookie() {
        return createTokenCookie(cookieProperties.getAccessTokenName(), "", 0);
    }

    public ResponseCookie createExpiredRefreshTokenCookie() {
        return createTokenCookie(cookieProperties.getRefreshTokenName(), "", 0);
    }

    private ResponseCookie createTokenCookie(String name, String value, Integer maxAgeSeconds) {
        return ResponseCookie.from(name, value)
            .httpOnly(cookieProperties.isHttpOnly())
            .secure(cookieProperties.isSecure())
            .sameSite(cookieProperties.getSameSite())
            .path(COOKIE_PATH)
            .maxAge(Duration.ofSeconds(maxAgeSeconds))
            .build();
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
