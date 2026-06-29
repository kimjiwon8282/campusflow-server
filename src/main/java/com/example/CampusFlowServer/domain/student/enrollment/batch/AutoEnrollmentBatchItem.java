package com.example.CampusFlowServer.domain.student.enrollment.batch;

public record AutoEnrollmentBatchItem(
    Long wishCourseId,
    Long courseOfferingId
) {
}//Reader와 Writer사이에 전달되는 데이터 객체
