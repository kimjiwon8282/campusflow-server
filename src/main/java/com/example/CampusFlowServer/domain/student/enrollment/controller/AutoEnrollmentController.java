package com.example.CampusFlowServer.domain.student.enrollment.controller;

import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentApplyRequest;
import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentApplyResponse;
import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentBatchLaunchResponse;
import com.example.CampusFlowServer.domain.student.enrollment.service.AutoEnrollmentBatchLaunchService;
import com.example.CampusFlowServer.domain.student.enrollment.service.AutoEnrollmentService;
import com.example.CampusFlowServer.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Staff Auto Enrollment", description = "Staff auto enrollment reflection API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff/auto-enrollments")
public class AutoEnrollmentController {

    private final AutoEnrollmentService autoEnrollmentService;
    private final AutoEnrollmentBatchLaunchService autoEnrollmentBatchLaunchService;

    @PostMapping("/apply")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
        summary = "Apply done wish courses as auto enrollments",
        security = @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    )
    public AutoEnrollmentApplyResponse applyAutoEnrollments(
        @RequestBody AutoEnrollmentApplyRequest request
    ) {
        return autoEnrollmentService.applyAutoEnrollments(request.year(), request.term());
    }

    @PostMapping("/pre-apply-batch")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(
        summary = "Launch auto-enrollment pre-apply batch",
        security = @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    )
    public AutoEnrollmentBatchLaunchResponse launchPreApplyBatch(
        @RequestBody AutoEnrollmentApplyRequest request
    ) {
        return autoEnrollmentBatchLaunchService.launch(request.year(), request.term());
    }
}
