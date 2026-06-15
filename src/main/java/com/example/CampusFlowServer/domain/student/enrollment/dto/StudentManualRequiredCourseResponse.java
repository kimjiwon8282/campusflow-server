package com.example.CampusFlowServer.domain.student.enrollment.dto;

public record StudentManualRequiredCourseResponse(
    Long wishCourseId,
    Long courseOfferingId,
    String subjectCode,
    String subjectName,
    String professorName,
    Integer credit,
    String courseTimeText,
    String result,
    String resultMessage,
    Boolean actionAvailable
) {
}
