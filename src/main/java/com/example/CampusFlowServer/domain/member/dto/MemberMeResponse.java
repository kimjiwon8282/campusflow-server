package com.example.CampusFlowServer.domain.member.dto;

public record MemberMeResponse(
    Long memberId,
    String loginId,
    String name,
    String role
) {
}
