package com.example.CampusFlowServer.domain.auth.repository;

import com.example.CampusFlowServer.domain.auth.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    List<RefreshToken> findAllByMemberIdAndRevokedFalse(Long memberId);

    boolean existsByTokenHash(String tokenHash);

    List<RefreshToken> findAllByExpiresAtBefore(LocalDateTime now);

    List<RefreshToken> findAllByMemberId(Long memberId);

    long deleteByExpiresAtBefore(LocalDateTime now);

    long deleteByRevokedTrueAndRevokedAtBefore(LocalDateTime cutoff);
}
