package com.example.CampusFlowServer.domain.student.enrollment.dto;

public record AutoEnrollmentApplyResponse(
    Integer targetCount,
    Integer createdCount,
    Integer skippedCount
) {
}
