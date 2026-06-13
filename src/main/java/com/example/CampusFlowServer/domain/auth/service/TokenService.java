package com.example.CampusFlowServer.domain.auth.service;

import com.example.CampusFlowServer.domain.auth.dto.TokenPair;
import com.example.CampusFlowServer.domain.auth.entity.RefreshToken;
import com.example.CampusFlowServer.domain.auth.exception.AuthErrorCode;
import com.example.CampusFlowServer.domain.auth.exception.AuthException;
import com.example.CampusFlowServer.domain.auth.repository.RefreshTokenRepository;
import com.example.CampusFlowServer.domain.auth.token.JwtTokenProvider;
import com.example.CampusFlowServer.domain.auth.token.RefreshTokenHasher;
import com.example.CampusFlowServer.domain.member.entity.Member;
import com.example.CampusFlowServer.domain.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenHasher refreshTokenHasher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public TokenPair issueTokens(Member member, String userAgent, String ipAddress) {
        String accessToken = jwtTokenProvider.createAccessToken(member);
        String refreshToken = jwtTokenProvider.createRefreshToken(member);
        saveRefreshToken(member, refreshToken, userAgent, ipAddress);

        return new TokenPair(accessToken, refreshToken);
    }

    @Transactional
    public TokenPair reissue(String rawRefreshToken, String userAgent, String ipAddress) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }

        Long memberId = jwtTokenProvider.getMemberIdFromRefreshToken(rawRefreshToken);
        String tokenHash = refreshTokenHasher.hash(rawRefreshToken);
        RefreshToken storedRefreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_TOKEN));

        if (storedRefreshToken.isRevoked()) {
            throw new AuthException(AuthErrorCode.REVOKED_TOKEN);
        }

        LocalDateTime now = LocalDateTime.now();
        if (!storedRefreshToken.getExpiresAt().isAfter(now)) {
            storedRefreshToken.revoke(now);
            throw new AuthException(AuthErrorCode.EXPIRED_TOKEN);
        }

        storedRefreshToken.revoke(now);

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_TOKEN));

        String accessToken = jwtTokenProvider.createAccessToken(member);
        String refreshToken = jwtTokenProvider.createRefreshToken(member);
        saveRefreshToken(member, refreshToken, userAgent, ipAddress);

        return new TokenPair(accessToken, refreshToken);
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            return;
        }

        String tokenHash = refreshTokenHasher.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
            .filter(refreshToken -> !refreshToken.isRevoked())
            .ifPresent(refreshToken -> refreshToken.revoke(LocalDateTime.now()));
    }

    @Transactional
    public void revokeAllByMemberId(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.findAllByMemberIdAndRevokedFalse(memberId)
            .forEach(refreshToken -> refreshToken.revoke(now));
    }

    private void saveRefreshToken(Member member, String rawRefreshToken, String userAgent, String ipAddress) {
        String tokenHash = refreshTokenHasher.hash(rawRefreshToken);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
            jwtTokenProvider.parseRefreshToken(rawRefreshToken).getExpiration().toInstant(),
            ZoneId.systemDefault()
        );

        RefreshToken refreshToken = RefreshToken.create(
            member,
            tokenHash,
            expiresAt,
            userAgent,
            ipAddress
        );
        refreshTokenRepository.save(refreshToken);
    }
}
