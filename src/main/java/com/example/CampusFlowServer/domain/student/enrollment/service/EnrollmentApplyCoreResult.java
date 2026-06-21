package com.example.CampusFlowServer.domain.student.enrollment.service;

import com.example.CampusFlowServer.domain.enrollment.enums.EnrollmentSource;
import com.example.CampusFlowServer.domain.enrollment.enums.EnrollmentStatus;

record EnrollmentApplyCoreResult(
    Long enrollmentId,
    Long courseOfferingId,
    String subjectCode,
    String subjectName,
    EnrollmentStatus status,
    EnrollmentSource source
) {
}
