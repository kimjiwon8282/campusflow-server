package com.example.CampusFlowServer.domain.student.enrollment.batch;

public record AutoEnrollmentBatchItem(
    Long wishCourseId,
    Long courseOfferingId
) {
}
