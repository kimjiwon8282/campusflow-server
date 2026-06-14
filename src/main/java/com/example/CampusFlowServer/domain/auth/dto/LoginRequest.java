package com.example.CampusFlowServer.domain.auth.dto;

public record LoginRequest(
    String loginId,
    String password
) {
}
