package com.example.CampusFlowServer.domain.auth.controller;

import com.example.CampusFlowServer.domain.auth.cookie.AuthCookieManager;
import com.example.CampusFlowServer.domain.auth.dto.AuthMessageResponse;
import com.example.CampusFlowServer.domain.auth.dto.TokenPair;
import com.example.CampusFlowServer.domain.auth.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthCookieManager authCookieManager;
    private final TokenService tokenService;

    @PostMapping("/reissue")
    public AuthMessageResponse reissue(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String refreshToken = authCookieManager.extractRefreshToken(request);
        TokenPair tokenPair = tokenService.reissue(
            refreshToken,
            request.getHeader(HttpHeaders.USER_AGENT),
            extractClientIp(request)
        );
        authCookieManager.addTokenCookies(response, tokenPair);

        return new AuthMessageResponse("Token reissued");
    }

    @PostMapping("/logout")
    public AuthMessageResponse logout(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String refreshToken = authCookieManager.extractRefreshToken(request);
        tokenService.revoke(refreshToken);
        authCookieManager.expireTokenCookies(response);

        return new AuthMessageResponse("Logged out");
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
