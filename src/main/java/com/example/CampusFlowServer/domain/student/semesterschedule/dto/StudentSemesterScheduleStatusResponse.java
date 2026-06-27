package com.example.CampusFlowServer.domain.student.semesterschedule.dto;

public record StudentSemesterScheduleStatusResponse(
    boolean enrollmentOpen,
    boolean wishOpen,
    String enrollmentMessage,
    String wishMessage
) {
}
