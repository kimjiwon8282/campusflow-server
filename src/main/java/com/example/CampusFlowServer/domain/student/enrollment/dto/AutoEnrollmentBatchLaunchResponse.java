package com.example.CampusFlowServer.domain.student.enrollment.dto;

import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record AutoEnrollmentBatchLaunchResponse(//배치의 실행 결과를 담는 DTO
    Long jobExecutionId,
    String jobName,
    String status,
    Long semesterId,
    Integer targetYear,
    SemesterTerm targetTerm,
    LocalDateTime enrollmentStartAt,
    LocalDate businessDate,
    long targetCount,
    long appliedCount,
    long skippedCount,
    long failedCount,
    Map<String, Long> statusCounts
) {
}
