package com.example.CampusFlowServer.domain.auth.filter;

import com.example.CampusFlowServer.domain.auth.cookie.AuthCookieManager;
import com.example.CampusFlowServer.domain.auth.exception.AuthErrorCode;
import com.example.CampusFlowServer.domain.auth.exception.AuthException;
import com.example.CampusFlowServer.domain.auth.handler.AuthSecurityResponseWriter;
import com.example.CampusFlowServer.domain.auth.security.CustomMemberDetails;
import com.example.CampusFlowServer.domain.auth.token.JwtTokenProvider;
import com.example.CampusFlowServer.domain.member.enums.MemberRole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String MEMBER_ID_CLAIM = "memberId";
    private static final String LOGIN_ID_CLAIM = "loginId";
    private static final String NAME_CLAIM = "name";
    private static final String ROLE_CLAIM = "role";
    private static final String API_V1_PREFIX = "/api/v1/";
    private static final Set<String> EXCLUDED_PATHS = Set.of(
        "/api/v1/auth/login",
        "/api/v1/auth/reissue",
        "/api/v1/auth/logout",
        "/api/v1/csrf"
    );

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthCookieManager authCookieManager;
    private final AuthSecurityResponseWriter responseWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (!servletPath.startsWith(API_V1_PREFIX)) {
            return true;
        }

        return EXCLUDED_PATHS.contains(servletPath);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String accessToken = authCookieManager.extractAccessToken(request);
        if (accessToken == null || accessToken.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtTokenProvider.parseAccessToken(accessToken);
            CustomMemberDetails memberDetails = CustomMemberDetails.fromToken(
                extractMemberId(claims),
                extractRequiredString(claims, LOGIN_ID_CLAIM),
                extractRequiredString(claims, NAME_CLAIM),
                MemberRole.valueOf(extractRequiredString(claims, ROLE_CLAIM))
            );

            UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                    memberDetails,
                    null,
                    memberDetails.getAuthorities()
                );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (AuthException e) {
            writeTokenError(response, e.getErrorCode());
        } catch (RuntimeException e) {
            writeTokenError(response, AuthErrorCode.INVALID_TOKEN);
        }
    }

    private Long extractMemberId(Claims claims) {
        Object value = claims.get(MEMBER_ID_CLAIM);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new AuthException(AuthErrorCode.INVALID_TOKEN);
    }

    private String extractRequiredString(Claims claims, String claimName) {
        String value = claims.get(claimName, String.class);
        if (!StringUtils.hasText(value)) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
        return value;
    }

    private void writeTokenError(
        HttpServletResponse response,
        AuthErrorCode errorCode
    ) throws IOException {
        SecurityContextHolder.clearContext();
        responseWriter.write(response, errorCode);
    }
}
