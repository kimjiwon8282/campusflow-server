package com.example.CampusFlowServer.domain.student.wishcourse.exception;

import lombok.Getter;

@Getter
public class StudentWishCourseException extends RuntimeException {

    private final StudentWishCourseErrorCode errorCode;

    public StudentWishCourseException(StudentWishCourseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
