package com.example.CampusFlowServer.domain.enrollment.repository;

public interface WishCourseCountProjection {

    Long getCourseOfferingId();

    Long getWishCount();

    Long getAutoApplyCount();
}
