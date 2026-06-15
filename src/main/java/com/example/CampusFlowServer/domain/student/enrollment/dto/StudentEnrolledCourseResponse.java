package com.example.CampusFlowServer.domain.student.enrollment.dto;

public record StudentEnrolledCourseResponse(
    Long enrollmentId,
    Long courseOfferingId,
    String subjectCode,
    String subjectName,
    String professorName,
    Integer credit,
    String courseTimeText,
    String source
) {
}
