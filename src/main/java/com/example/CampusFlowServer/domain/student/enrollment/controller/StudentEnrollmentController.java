package com.example.CampusFlowServer.domain.student.enrollment.controller;

import com.example.CampusFlowServer.domain.auth.security.CustomMemberDetails;
import com.example.CampusFlowServer.domain.student.catalog.dto.StudentCatalogCourseSearchRequest;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentApplyRequest;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentApplyResponse;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentCancelResponse;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentCourseResponse;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentListResponse;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentSemesterRequest;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentManualRequiredCourseResponse;
import com.example.CampusFlowServer.domain.student.enrollment.service.StudentEnrollmentService;
import com.example.CampusFlowServer.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Student Enrollment", description = "Student enrollment read API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/student/enrollments")
public class StudentEnrollmentController {

    private final StudentEnrollmentService studentEnrollmentService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
        summary = "Apply enrollment",
        security = @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    )
    public StudentEnrollmentApplyResponse apply(
        @AuthenticationPrincipal CustomMemberDetails memberDetails,
        @RequestBody StudentEnrollmentApplyRequest request
    ) {
        return studentEnrollmentService.apply(memberDetails.getMemberId(), request);
    }

    @DeleteMapping("/{enrollmentId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
        summary = "Cancel enrollment",
        security = @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    )
    public StudentEnrollmentCancelResponse cancel(
        @AuthenticationPrincipal CustomMemberDetails memberDetails,
        @PathVariable Long enrollmentId
    ) {
        return studentEnrollmentService.cancel(memberDetails.getMemberId(), enrollmentId);
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
        summary = "Get my enrollments",
        security = @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    )
    public StudentEnrollmentListResponse getMyEnrollments(
        @AuthenticationPrincipal CustomMemberDetails memberDetails,
        @ParameterObject @ModelAttribute StudentEnrollmentSemesterRequest request
    ) {
        return studentEnrollmentService.getMyEnrollments(memberDetails.getMemberId(), request);
    }

    @GetMapping("/manual-required")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
        summary = "Get manual required wish courses",
        security = @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    )
    public List<StudentManualRequiredCourseResponse> getManualRequiredCourses(
        @AuthenticationPrincipal CustomMemberDetails memberDetails,
        @ParameterObject @ModelAttribute StudentEnrollmentSemesterRequest request
    ) {
        return studentEnrollmentService.getManualRequiredCourses(
            memberDetails.getMemberId(),
            request
        );
    }

    @GetMapping("/courses")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
        summary = "Search courses for enrollment",
        security = @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    )
    public List<StudentEnrollmentCourseResponse> searchCourses(
        @AuthenticationPrincipal CustomMemberDetails memberDetails,
        @ParameterObject @ModelAttribute StudentCatalogCourseSearchRequest request
    ) {
        return studentEnrollmentService.searchCourses(memberDetails.getMemberId(), request);
    }
}
