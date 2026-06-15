package com.example.CampusFlowServer.domain.enrollment.repository;

import com.example.CampusFlowServer.domain.enrollment.entity.CoursePrerequisite;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoursePrerequisiteRepository extends JpaRepository<CoursePrerequisite, Long> {

    @EntityGraph(attributePaths = {"subject", "prerequisiteSubject"})
    List<CoursePrerequisite> findBySubjectIdInAndActiveTrue(Collection<Long> subjectIds);
}
