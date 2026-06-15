package com.example.CampusFlowServer.domain.student.wishcourse.dto;

import com.example.CampusFlowServer.domain.enrollment.enums.WishAutoApplyResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 희망과목 목록 항목 응답. 실제 수강신청 확정 정보가 아닙니다.")
public record StudentWishCourseItemResponse(
    @Schema(description = "희망과목 ID", example = "1")
    Long wishCourseId,
    @Schema(description = "개설 강의 ID", example = "1")
    Long courseOfferingId,
    @Schema(description = "교과목 코드", example = "CSE101")
    String subjectCode,
    @Schema(description = "교과목명", example = "자료구조")
    String subjectName,
    @Schema(description = "개설 학과명", example = "컴퓨터공학과")
    String departmentName,
    @Schema(description = "담당 교수명", example = "박교수")
    String professorName,
    @Schema(description = "학점", example = "3")
    Integer credit,
    @Schema(description = "강의 시간 표시 문자열", example = "월 1~2교시, 수 3~4교시")
    String courseTimeText,
    @Schema(description = "정원", example = "30")
    Integer capacity,
    @Schema(description = "해당 개설 강의를 희망과목으로 담은 전체 학생 수", example = "12")
    Long wishCount,
    @Schema(description = "해당 개설 강의의 자동신청 선택 학생 수", example = "4")
    Long autoApplyCount,
    @Schema(description = "현재 학생의 자동신청 선택 여부", example = "true")
    boolean autoApply,
    @Schema(description = "현재 학생의 자동신청 결과", example = "PENDING")
    WishAutoApplyResult result,
    @Schema(description = "현재 학생의 자동신청 결과 메시지", example = "자동신청 대기 중입니다.")
    String resultMessage
) {
}
