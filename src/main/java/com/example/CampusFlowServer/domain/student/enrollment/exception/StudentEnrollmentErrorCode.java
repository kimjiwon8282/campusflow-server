package com.example.CampusFlowServer.domain.student.enrollment.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StudentEnrollmentErrorCode {

    INVALID_SEARCH_MODE(
        "ENROLLMENT_001",
        "Search mode must be condition or direct.",
        HttpStatus.BAD_REQUEST
    ),
    REQUIRED_YEAR(
        "ENROLLMENT_002",
        "Year is required.",
        HttpStatus.BAD_REQUEST
    ),
    REQUIRED_TERM(
        "ENROLLMENT_003",
        "Term is required.",
        HttpStatus.BAD_REQUEST
    ),
    STUDENT_PROFILE_NOT_FOUND(
        "ENROLLMENT_004",
        "Student profile was not found.",
        HttpStatus.BAD_REQUEST
    ),
    SEMESTER_NOT_FOUND(
        "ENROLLMENT_005",
        "Semester was not found.",
        HttpStatus.BAD_REQUEST
    ),
    REQUIRED_COURSE_OFFERING(
        "ENROLLMENT_006",
        "Course offering is required.",
        HttpStatus.BAD_REQUEST
    ),
    COURSE_OFFERING_NOT_FOUND(
        "ENROLLMENT_007",
        "Course offering was not found.",
        HttpStatus.NOT_FOUND
    ),
    ENROLLMENT_PERIOD_CLOSED(
        "ENROLLMENT_008",
        "Enrollment period is closed.",
        HttpStatus.BAD_REQUEST
    ),
    DUPLICATE_ENROLLMENT(
        "ENROLLMENT_009",
        "Already enrolled or waiting for this course.",
        HttpStatus.BAD_REQUEST
    ),
    GRADE_RESTRICTED(
        "ENROLLMENT_010",
        "Grade is restricted.",
        HttpStatus.BAD_REQUEST
    ),
    PREREQUISITE_REQUIRED(
        "ENROLLMENT_011",
        "Prerequisite is required.",
        HttpStatus.BAD_REQUEST
    ),
    CREDIT_LIMIT_EXCEEDED(
        "ENROLLMENT_012",
        "Credit limit exceeded.",
        HttpStatus.BAD_REQUEST
    ),
    TIME_CONFLICT(
        "ENROLLMENT_013",
        "Course time conflicts with an enrolled course.",
        HttpStatus.BAD_REQUEST
    ),
    ENROLLMENT_NOT_FOUND(
        "ENROLLMENT_014",
        "Enrollment was not found.",
        HttpStatus.NOT_FOUND
    ),
    ENROLLMENT_ACCESS_DENIED(
        "ENROLLMENT_015",
        "Only own enrollment can be cancelled.",
        HttpStatus.FORBIDDEN
    ),
    ENROLLMENT_NOT_CANCELABLE(
        "ENROLLMENT_016",
        "Only enrolled or waiting enrollment can be cancelled.",
        HttpStatus.BAD_REQUEST
    );

    private final String code;
    private final String message;
    private final HttpStatus status;
}
