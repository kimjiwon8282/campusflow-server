package com.example.CampusFlowServer.domain.student.wishcourse.dto;

import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "희망과목 담기 대상 강의 검색 조건")
public record StudentWishCourseCandidateSearchRequest(
    @Schema(description = "검색 모드. condition 또는 direct. 생략, blank, all이면 condition으로 처리하며 그 외 값은 WISH_001 에러", example = "condition")
    String mode,
    @Schema(description = "학년도", example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
    Integer year,
    @Schema(description = "학기 구분", example = "FIRST", requiredMode = Schema.RequiredMode.REQUIRED)
    SemesterTerm term,
    @Schema(description = "단과대명. blank 또는 all이면 조건 제외", example = "공과대학")
    String collegeName,
    @Schema(description = "전공/학과명 또는 개설 학과명. blank 또는 all이면 조건 제외", example = "컴퓨터공학과")
    String departmentName,
    @Schema(description = "과목 구분 코드. SubjectCategory enum name 기준. blank 또는 all이면 조건 제외", example = "MAJOR_REQUIRED")
    String category,
    @Schema(description = "교과목명 직접 검색어. blank 또는 all이면 조건 제외", example = "자료구조")
    String subjectName,
    @Schema(description = "담당 교수명 직접 검색어. blank 또는 all이면 조건 제외", example = "박교수")
    String professorName
) {
}
