package com.example.CampusFlowServer.domain.student.semesterschedule.controller;

import com.example.CampusFlowServer.domain.student.semesterschedule.dto.StudentSemesterScheduleStatusRequest;
import com.example.CampusFlowServer.domain.student.semesterschedule.dto.StudentSemesterScheduleStatusResponse;
import com.example.CampusFlowServer.domain.student.semesterschedule.service.StudentSemesterScheduleService;
import com.example.CampusFlowServer.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Student Semester Schedule", description = "Student semester schedule status API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/student/semester-schedules")
public class StudentSemesterScheduleController {

    private final StudentSemesterScheduleService studentSemesterScheduleService;

    @GetMapping("/status")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
        summary = "Get enrollment and wish period status",
        security = @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    )
    public StudentSemesterScheduleStatusResponse getStatus(
        @ParameterObject @ModelAttribute StudentSemesterScheduleStatusRequest request
    ) {
        return studentSemesterScheduleService.getStatus(request);
    }
}
