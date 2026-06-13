package com.example.CampusFlowServer.domain.auth.token;

import com.example.CampusFlowServer.domain.auth.exception.AuthErrorCode;
import com.example.CampusFlowServer.domain.auth.exception.AuthException;
import com.example.CampusFlowServer.domain.auth.properties.JwtProperties;
import com.example.CampusFlowServer.domain.auth.security.CustomMemberDetails;
import com.example.CampusFlowServer.domain.member.entity.Member;
import com.example.CampusFlowServer.domain.member.enums.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String MEMBER_ID_CLAIM = "memberId";
    private static final String LOGIN_ID_CLAIM = "loginId";
    private static final String NAME_CLAIM = "name";
    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final JwtProperties jwtProperties;

    public String createAccessToken(CustomMemberDetails memberDetails) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMillis(jwtProperties.getAccessTokenValidityMs()));

        return Jwts.builder()
            .subject(memberDetails.getUsername())
            .claim(MEMBER_ID_CLAIM, memberDetails.getMemberId())
            .claim(LOGIN_ID_CLAIM, memberDetails.getLoginId())
            .claim(NAME_CLAIM, memberDetails.getName())
            .claim(ROLE_CLAIM, memberDetails.getRole().name())
            .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(accessSecretKey(), Jwts.SIG.HS256)
            .compact();
    }

    public String createAccessToken(Member member) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMillis(jwtProperties.getAccessTokenValidityMs()));

        return Jwts.builder()
            .subject(member.getLoginId())
            .claim(MEMBER_ID_CLAIM, member.getId())
            .claim(LOGIN_ID_CLAIM, member.getLoginId())
            .claim(NAME_CLAIM, member.getName())
            .claim(ROLE_CLAIM, member.getRole().name())
            .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(accessSecretKey(), Jwts.SIG.HS256)
            .compact();
    }

    public String createRefreshToken(Member member) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMillis(jwtProperties.getRefreshTokenValidityMs()));

        return Jwts.builder()
            .subject(member.getLoginId())
            .claim(MEMBER_ID_CLAIM, member.getId())
            .claim(LOGIN_ID_CLAIM, member.getLoginId())
            .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(refreshSecretKey(), Jwts.SIG.HS256)
            .compact();
    }

    public boolean validateAccessToken(String token) {
        return validate(token, ACCESS_TOKEN_TYPE, accessSecretKey());
    }

    public boolean validateRefreshToken(String token) {
        return validate(token, REFRESH_TOKEN_TYPE, refreshSecretKey());
    }

    public Claims parseAccessToken(String token) {
        return parse(token, ACCESS_TOKEN_TYPE, accessSecretKey());
    }

    public Claims parseRefreshToken(String token) {
        return parse(token, REFRESH_TOKEN_TYPE, refreshSecretKey());
    }

    public Long getMemberIdFromAccessToken(String accessToken) {
        return getLongClaim(parseAccessToken(accessToken), MEMBER_ID_CLAIM);
    }

    public Long getMemberIdFromRefreshToken(String refreshToken) {
        return getLongClaim(parseRefreshToken(refreshToken), MEMBER_ID_CLAIM);
    }

    public String getLoginIdFromAccessToken(String accessToken) {
        return parseAccessToken(accessToken).get(LOGIN_ID_CLAIM, String.class);
    }

    public String getLoginIdFromRefreshToken(String refreshToken) {
        return parseRefreshToken(refreshToken).get(LOGIN_ID_CLAIM, String.class);
    }

    public MemberRole getRoleFromAccessToken(String accessToken) {
        String role = parseAccessToken(accessToken).get(ROLE_CLAIM, String.class);
        return MemberRole.valueOf(role);
    }

    private boolean validate(String token, String expectedTokenType, SecretKey secretKey) {
        try {
            parse(token, expectedTokenType, secretKey);
            return true;
        } catch (AuthException e) {
            return false;
        }
    }

    private Claims parse(String token, String expectedTokenType, SecretKey secretKey) {
        if (!StringUtils.hasText(token)) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }

        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            if (!expectedTokenType.equals(tokenType)) {
                throw new AuthException(AuthErrorCode.INVALID_TOKEN);
            }

            return claims;
        } catch (ExpiredJwtException e) {
            throw new AuthException(AuthErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private Long getLongClaim(Claims claims, String claimName) {
        Object value = claims.get(claimName);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new AuthException(AuthErrorCode.INVALID_TOKEN);
    }

    private SecretKey accessSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getAccessSecret().getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey refreshSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getRefreshSecret().getBytes(StandardCharsets.UTF_8));
    }
}
