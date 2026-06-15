package com.example.CampusFlowServer.domain.student.wishcourse.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StudentWishCourseErrorCode {

    INVALID_SEARCH_MODE(
        "WISH_001",
        "검색 모드는 condition 또는 direct만 사용할 수 있습니다.",
        HttpStatus.BAD_REQUEST
    ),
    REQUIRED_YEAR(
        "WISH_002",
        "학년도는 필수입니다.",
        HttpStatus.BAD_REQUEST
    ),
    REQUIRED_TERM(
        "WISH_003",
        "학기 구분은 필수입니다.",
        HttpStatus.BAD_REQUEST
    ),
    STUDENT_PROFILE_NOT_FOUND(
        "WISH_004",
        "학생 프로필을 찾을 수 없습니다.",
        HttpStatus.BAD_REQUEST
    ),
    SEMESTER_NOT_FOUND(
        "WISH_005",
        "학기를 찾을 수 없습니다.",
        HttpStatus.BAD_REQUEST
    ),
    COURSE_OFFERING_NOT_FOUND(
        "WISH_006",
        "존재하지 않는 개설 강의입니다.",
        HttpStatus.NOT_FOUND
    ),
    WISHLIST_PERIOD_CLOSED(
        "WISH_007",
        "희망과목 담기 기간이 아닙니다.",
        HttpStatus.BAD_REQUEST
    ),
    DUPLICATE_WISH_COURSE(
        "WISH_008",
        "이미 희망과목에 담긴 강의입니다.",
        HttpStatus.BAD_REQUEST
    ),
    WISH_COURSE_NOT_FOUND(
        "WISH_009",
        "희망과목을 찾을 수 없습니다.",
        HttpStatus.NOT_FOUND
    ),
    WISH_COURSE_ACCESS_DENIED(
        "WISH_010",
        "본인의 희망과목만 수정할 수 있습니다.",
        HttpStatus.FORBIDDEN
    ),
    REQUIRED_AUTO_APPLY(
        "WISH_011",
        "자동신청 선택 여부는 필수입니다.",
        HttpStatus.BAD_REQUEST
    );

    private final String code;
    private final String message;
    private final HttpStatus status;
}
