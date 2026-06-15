package com.example.CampusFlowServer.domain.student.wishcourse.dto;

import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 희망과목 학기 조회 조건")
public record StudentWishCourseSemesterRequest(
    @Schema(description = "학년도", example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
    Integer year,
    @Schema(description = "학기 구분", example = "FIRST", requiredMode = Schema.RequiredMode.REQUIRED)
    SemesterTerm term
) {
}
