package com.example.CampusFlowServer.domain.auth.entity;

import com.example.CampusFlowServer.global.common.BaseEntity;
import com.example.CampusFlowServer.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "refresh_tokens",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_refresh_token_hash", columnNames = "token_hash")
    },
    indexes = {
        @Index(name = "idx_refresh_token_member_revoked", columnList = "member_id, revoked"),
        @Index(name = "idx_refresh_token_expires_at", columnList = "expires_at")
    }
)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    private LocalDateTime revokedAt;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    public static RefreshToken create(
        Member member,
        String tokenHash,
        LocalDateTime expiresAt,
        String userAgent,
        String ipAddress
    ) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.member = member;
        refreshToken.tokenHash = tokenHash;
        refreshToken.expiresAt = expiresAt;
        refreshToken.userAgent = userAgent;
        refreshToken.ipAddress = ipAddress;
        refreshToken.revoked = false;
        return refreshToken;
    }

    public void revoke(LocalDateTime revokedAt) {
        this.revoked = true;
        this.revokedAt = revokedAt;
    }
}
