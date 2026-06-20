package com.example.CampusFlowServer.domain.student.enrollment.dto;

public record StudentEnrollmentSummaryResponse(
    String semesterName,
    Integer currentCredit,
    Integer activeCredit,
    Integer maxCredit,
    Integer remainingCredit,
    Integer enrolledCount,
    Integer waitingCount
) {
}
