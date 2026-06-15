package com.example.CampusFlowServer.domain.student.enrollment.dto;

public record StudentEnrollmentCancelResponse(
    Long enrollmentId,
    Long courseOfferingId,
    String status,
    Long promotedEnrollmentId,
    String message
) {
}
