package com.example.CampusFlowServer.domain.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode {

    INVALID_TOKEN("AUTH_001", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("AUTH_002", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    REVOKED_TOKEN("AUTH_003", "폐기된 토큰입니다.", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
