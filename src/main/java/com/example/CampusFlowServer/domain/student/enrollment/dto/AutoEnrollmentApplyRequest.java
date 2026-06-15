package com.example.CampusFlowServer.domain.student.enrollment.dto;

import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;

public record AutoEnrollmentApplyRequest(
    Integer year,
    SemesterTerm term
) {
}
