package com.example.CampusFlowServer.domain.student.wishcourse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "내 희망과목 목록 응답")
public record StudentWishCourseListResponse(
    @Schema(description = "희망과목 요약")
    StudentWishCourseSummaryResponse summary,
    @Schema(description = "희망과목 목록")
    List<StudentWishCourseItemResponse> items
) {
}
