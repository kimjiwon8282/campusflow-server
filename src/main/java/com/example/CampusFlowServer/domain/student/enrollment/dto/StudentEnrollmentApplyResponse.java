package com.example.CampusFlowServer.domain.student.enrollment.dto;

public record StudentEnrollmentApplyResponse(
    Long enrollmentId,
    Long courseOfferingId,
    String subjectCode,
    String subjectName,
    String status,
    String source,
    Integer waitNo,
    String message
) {
}
