package com.example.CampusFlowServer.domain.courseplan.repository;

import com.example.CampusFlowServer.domain.courseplan.entity.CoursePlanContent;
import com.example.CampusFlowServer.domain.courseplan.enums.CoursePlanVersionType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoursePlanContentRepository extends JpaRepository<CoursePlanContent, Long> {

    List<CoursePlanContent> findByCoursePlanIdInAndVersionType(
        Collection<Long> coursePlanIds,
        CoursePlanVersionType versionType
    );
}
