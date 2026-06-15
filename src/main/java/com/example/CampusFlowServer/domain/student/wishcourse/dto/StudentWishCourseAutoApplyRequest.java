package com.example.CampusFlowServer.domain.student.wishcourse.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "희망과목 자동신청 선택/해제 요청")
public record StudentWishCourseAutoApplyRequest(
    @Schema(description = "자동신청 선택 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    Boolean autoApply
) {
}
