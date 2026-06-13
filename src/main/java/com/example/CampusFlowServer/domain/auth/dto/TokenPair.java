package com.example.CampusFlowServer.domain.auth.dto;

public record TokenPair(
    String accessToken,
    String refreshToken
) {
}
