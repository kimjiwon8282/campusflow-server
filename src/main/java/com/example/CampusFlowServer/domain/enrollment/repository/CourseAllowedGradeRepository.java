package com.example.CampusFlowServer.domain.enrollment.repository;

import com.example.CampusFlowServer.domain.enrollment.entity.CourseAllowedGrade;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseAllowedGradeRepository extends JpaRepository<CourseAllowedGrade, Long> {

    @EntityGraph(attributePaths = {"courseOffering"})
    List<CourseAllowedGrade> findByCourseOfferingIdIn(Collection<Long> courseOfferingIds);
}
