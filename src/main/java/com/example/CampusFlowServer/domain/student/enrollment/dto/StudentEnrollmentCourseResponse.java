package com.example.CampusFlowServer.domain.student.enrollment.dto;

public record StudentEnrollmentCourseResponse(
    Long courseOfferingId,
    String subjectCode,
    String subjectName,
    String departmentName,
    String professorName,
    Integer credit,
    String courseTimeText,
    String allowedGradeText,
    String prerequisiteText,
    Long enrolledCount,
    Integer capacity,
    Long waitingCount,
    String status,
    String statusMessage
) {
}
