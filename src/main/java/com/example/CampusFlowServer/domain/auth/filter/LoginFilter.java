package com.example.CampusFlowServer.domain.auth.filter;

import com.example.CampusFlowServer.domain.auth.cookie.AuthCookieManager;
import com.example.CampusFlowServer.domain.auth.dto.LoginRequest;
import com.example.CampusFlowServer.domain.auth.dto.LoginResponse;
import com.example.CampusFlowServer.domain.auth.dto.TokenPair;
import com.example.CampusFlowServer.domain.auth.exception.AuthErrorCode;
import com.example.CampusFlowServer.domain.auth.handler.AuthSecurityResponseWriter;
import com.example.CampusFlowServer.domain.auth.security.CustomMemberDetails;
import com.example.CampusFlowServer.domain.auth.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String HTTP_METHOD = "POST";

    private final ObjectMapper objectMapper;
    private final TokenService tokenService;
    private final AuthCookieManager authCookieManager;
    private final AuthSecurityResponseWriter responseWriter;

    public LoginFilter(
        org.springframework.security.authentication.AuthenticationManager authenticationManager,
        ObjectMapper objectMapper,
        TokenService tokenService,
        AuthCookieManager authCookieManager,
        AuthSecurityResponseWriter responseWriter
    ) {
        this.objectMapper = objectMapper;
        this.tokenService = tokenService;
        this.authCookieManager = authCookieManager;
        this.responseWriter = responseWriter;
        setAuthenticationManager(authenticationManager);
        setRequiresAuthenticationRequestMatcher(request ->
            HTTP_METHOD.equals(request.getMethod()) && LOGIN_URL.equals(request.getServletPath())
        );
        setPostOnly(true);
    }

    @Override
    public Authentication attemptAuthentication(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws AuthenticationException {
        LoginRequest loginRequest;
        try {
            loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
        } catch (IOException e) {
            throw new AuthenticationServiceException("Invalid login request.");
        }

        String loginId = loginRequest.loginId() == null ? "" : loginRequest.loginId().trim();
        String password = loginRequest.password() == null ? "" : loginRequest.password();

        UsernamePasswordAuthenticationToken authenticationToken =
            UsernamePasswordAuthenticationToken.unauthenticated(loginId, password);
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        return getAuthenticationManager().authenticate(authenticationToken);
    }

    @Override
    protected void successfulAuthentication(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain,
        Authentication authResult
    ) throws IOException {
        CustomMemberDetails memberDetails = (CustomMemberDetails) authResult.getPrincipal();
        TokenPair tokenPair = tokenService.issueTokens(
            memberDetails.getMemberId(),
            request.getHeader(HttpHeaders.USER_AGENT),
            extractClientIp(request)
        );

        authCookieManager.addTokenCookies(response, tokenPair);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        LoginResponse loginResponse = new LoginResponse(
            memberDetails.getMemberId(),
            memberDetails.getLoginId(),
            memberDetails.getName(),
            memberDetails.getRole().name()
        );
        objectMapper.writeValue(response.getWriter(), loginResponse);
    }

    @Override
    protected void unsuccessfulAuthentication(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException failed
    ) throws IOException {
        responseWriter.write(response, AuthErrorCode.LOGIN_FAILED);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
