package com.example.CampusFlowServer.domain.student.enrollment.dto;

public record StudentWaitingCourseResponse(
    Long enrollmentId,
    Long courseOfferingId,
    String subjectCode,
    String subjectName,
    String professorName,
    String courseTimeText,
    Integer currentEnrollmentCount,
    Integer capacity,
    Integer waitNo
) {
}
