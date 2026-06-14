package com.example.CampusFlowServer.domain.auth.dto;

public record LoginResponse(
    Long memberId,
    String loginId,
    String name,
    String role
) {
}
