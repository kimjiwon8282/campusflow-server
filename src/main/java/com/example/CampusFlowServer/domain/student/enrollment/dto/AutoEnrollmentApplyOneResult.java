package com.example.CampusFlowServer.domain.student.enrollment.dto;

public record AutoEnrollmentApplyOneResult(
    Long wishCourseId,
    Long enrollmentId,
    AutoEnrollmentApplyOneStatus status,
    String message
) {

    public boolean applied() {
        return AutoEnrollmentApplyOneStatus.APPLIED.equals(status)
            || AutoEnrollmentApplyOneStatus.REACTIVATED.equals(status);
    }
}
