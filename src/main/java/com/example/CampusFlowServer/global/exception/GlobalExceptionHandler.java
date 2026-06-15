package com.example.CampusFlowServer.global.exception;

import com.example.CampusFlowServer.domain.auth.exception.AuthErrorCode;
import com.example.CampusFlowServer.domain.auth.exception.AuthException;
import com.example.CampusFlowServer.domain.student.catalog.exception.StudentCatalogErrorCode;
import com.example.CampusFlowServer.domain.student.catalog.exception.StudentCatalogException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(AuthException e) {
        AuthErrorCode errorCode = e.getErrorCode();
        return errorResponse(
            errorCode.getStatus(),
            errorCode.getCode(),
            errorCode.getMessage()
        );
    }

    @ExceptionHandler(StudentCatalogException.class)
    public ResponseEntity<Map<String, String>> handleStudentCatalogException(
        StudentCatalogException e
    ) {
        StudentCatalogErrorCode errorCode = e.getErrorCode();
        return errorResponse(
            errorCode.getStatus(),
            errorCode.getCode(),
            errorCode.getMessage()
        );
    }

    private ResponseEntity<Map<String, String>> errorResponse(
        HttpStatus status,
        String code,
        String message
    ) {
        return ResponseEntity
            .status(status)
            .body(Map.of(
                "code", code,
                "message", message
            ));
    }
}
