package com.example.CampusFlowServer.domain.student.enrollment.dto;

import java.util.List;

public record StudentEnrollmentListResponse(
    StudentEnrollmentSummaryResponse summary,
    List<StudentEnrolledCourseResponse> enrolledCourses,
    List<StudentWaitingCourseResponse> waitingCourses
) {
}
