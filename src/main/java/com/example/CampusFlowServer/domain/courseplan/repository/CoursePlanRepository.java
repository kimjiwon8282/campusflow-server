package com.example.CampusFlowServer.domain.courseplan.repository;

import com.example.CampusFlowServer.domain.courseplan.entity.CoursePlan;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoursePlanRepository extends JpaRepository<CoursePlan, Long> {

    @EntityGraph(attributePaths = "courseOffering")
    List<CoursePlan> findByCourseOfferingIdIn(Collection<Long> courseOfferingIds);
}
