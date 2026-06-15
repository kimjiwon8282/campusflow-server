package com.example.CampusFlowServer.domain.student.wishcourse.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 희망과목 요약 응답")
public record StudentWishCourseSummaryResponse(
    @Schema(description = "내 희망과목 전체 개수", example = "5")
    long wishCount,
    @Schema(description = "자동신청 선택 개수", example = "2")
    long autoApplySelectedCount,
    @Schema(description = "자동신청 완료 개수", example = "1")
    long autoDoneCount,
    @Schema(description = "본 신청 필요 개수", example = "3")
    long manualRequiredCount
) {
}
