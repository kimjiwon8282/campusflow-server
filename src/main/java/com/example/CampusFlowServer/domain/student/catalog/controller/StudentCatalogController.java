package com.example.CampusFlowServer.domain.student.catalog.controller;

import com.example.CampusFlowServer.domain.student.catalog.dto.StudentCatalogCourseResponse;
import com.example.CampusFlowServer.domain.student.catalog.dto.StudentCatalogCourseSearchRequest;
import com.example.CampusFlowServer.domain.student.catalog.service.StudentCatalogService;
import com.example.CampusFlowServer.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "학생 수강편람", description = "학생 수강편람 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/student/catalog")
public class StudentCatalogController {

    private final StudentCatalogService studentCatalogService;

    @GetMapping("/courses")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
        summary = "학생 수강편람 조회",
        description = "학년도와 학기를 기준으로 개설 강의를 조회한다. 수강편람은 수강신청 기간과 관계없이 상시 조회 가능하다.",
        security = @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    )
    public List<StudentCatalogCourseResponse> searchCourses(
        @ParameterObject @ModelAttribute StudentCatalogCourseSearchRequest request
    ) {
        return studentCatalogService.searchCourses(request);
    }
}
