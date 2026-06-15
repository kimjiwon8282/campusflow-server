package com.example.CampusFlowServer.domain.enrollment.repository;

import com.example.CampusFlowServer.domain.enrollment.enums.EnrollmentStatus;

public interface EnrollmentStatusCountProjection {

    Long getCourseOfferingId();

    EnrollmentStatus getStatus();

    Long getCount();
}
