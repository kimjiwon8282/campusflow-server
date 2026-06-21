package com.example.CampusFlowServer.domain.student.enrollment.service;

record EnrollmentPreValidationResult(
    Long studentId,
    Long courseOfferingId
) {
}
