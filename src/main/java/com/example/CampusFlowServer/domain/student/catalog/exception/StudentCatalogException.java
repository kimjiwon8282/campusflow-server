package com.example.CampusFlowServer.domain.student.catalog.exception;

import lombok.Getter;

@Getter
public class StudentCatalogException extends RuntimeException {

    private final StudentCatalogErrorCode errorCode;

    public StudentCatalogException(StudentCatalogErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
