package com.example.CampusFlowServer.domain.student.wishcourse.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "희망과목 담기 요청")
public record StudentWishCourseCreateRequest(
    @Schema(description = "개설 강의 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long courseOfferingId
) {
}
