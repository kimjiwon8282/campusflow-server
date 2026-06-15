package com.example.CampusFlowServer.domain.student.catalog.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StudentCatalogErrorCode {

    INVALID_SEARCH_MODE(
        "CATALOG_001",
        "검색 모드는 condition 또는 direct만 사용할 수 있습니다.",
        HttpStatus.BAD_REQUEST
    ),
    REQUIRED_YEAR(
        "CATALOG_002",
        "학년도는 필수입니다.",
        HttpStatus.BAD_REQUEST
    ),
    REQUIRED_TERM(
        "CATALOG_003",
        "학기 구분은 필수입니다.",
        HttpStatus.BAD_REQUEST
    );

    private final String code;
    private final String message;
    private final HttpStatus status;
}
