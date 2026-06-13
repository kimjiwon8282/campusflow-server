package com.example.CampusFlowServer.domain.auth.scheduler;

import com.example.CampusFlowServer.domain.auth.repository.RefreshTokenRepository;
import com.example.CampusFlowServer.domain.auth.properties.RefreshTokenCleanupProperties;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenCleanupProperties properties;

    @Transactional
    @Scheduled(cron = "${app.refresh-token-cleanup.cron}")
    public void cleanup() {
        if (!properties.isEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        long expiredCount = refreshTokenRepository.deleteByExpiresAtBefore(now);

        LocalDateTime revokedCutoff = now.minusDays(properties.getRevokedRetentionDays());
        long revokedCount = refreshTokenRepository.deleteByRevokedTrueAndRevokedAtBefore(revokedCutoff);

        log.info(
            "RefreshToken cleanup completed. expiredCount={}, revokedCount={}",
            expiredCount,
            revokedCount
        );
    }
}
