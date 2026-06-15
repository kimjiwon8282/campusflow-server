package com.example.CampusFlowServer.domain.student.enrollment.dto;

import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;

public record StudentEnrollmentSemesterRequest(
    @Schema(example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
    Integer year,
    @Schema(example = "FIRST", requiredMode = Schema.RequiredMode.REQUIRED)
    SemesterTerm term
) {
}
