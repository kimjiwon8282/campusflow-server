package com.example.CampusFlowServer.domain.student.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학생 수강편람 개설 강의 응답")
public record StudentCatalogCourseResponse(
    @Schema(description = "개설 강의 ID", example = "1")
    Long courseOfferingId,
    @Schema(description = "교과목 코드", example = "CSE101")
    String subjectCode,
    @Schema(description = "교과목명", example = "자료구조")
    String subjectName,
    @Schema(description = "개설 학과명", example = "컴퓨터공학과")
    String departmentName,
    @Schema(description = "단과대명", example = "공과대학")
    String collegeName,
    @Schema(description = "과목 구분 코드. SubjectCategory enum name 반환", example = "MAJOR_REQUIRED")
    String category,
    @Schema(description = "담당 교수명", example = "박교수")
    String professorName,
    @Schema(description = "학점", example = "3")
    Integer credit,
    @Schema(description = "강의 시간 표시 문자열", example = "월 1~2교시, 수 3~4교시")
    String courseTimeText,
    @Schema(description = "정원", example = "30")
    Integer capacity,
    @Schema(description = "강의계획서 상태. 없으면 EMPTY", example = "PUBLISHED")
    String coursePlanStatus,
    @Schema(description = "게시된 강의계획서 존재 여부", example = "true")
    boolean hasPublishedCoursePlan
) {
}
