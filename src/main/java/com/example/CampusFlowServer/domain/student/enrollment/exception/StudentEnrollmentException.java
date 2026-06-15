package com.example.CampusFlowServer.domain.student.enrollment.exception;

import lombok.Getter;

@Getter
public class StudentEnrollmentException extends RuntimeException {

    private final StudentEnrollmentErrorCode errorCode;

    public StudentEnrollmentException(StudentEnrollmentErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
