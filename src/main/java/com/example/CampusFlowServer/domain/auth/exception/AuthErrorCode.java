package com.example.CampusFlowServer.domain.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode {

    INVALID_TOKEN("AUTH_001", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("AUTH_002", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    REVOKED_TOKEN("AUTH_003", "폐기된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    LOGIN_FAILED("AUTH_004", "아이디 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    UNAUTHENTICATED("AUTH_005", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AUTH_006", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    CSRF_TOKEN_INVALID("AUTH_007", "유효하지 않은 요청입니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
