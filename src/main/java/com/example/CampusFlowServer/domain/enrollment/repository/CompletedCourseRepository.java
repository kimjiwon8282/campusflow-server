package com.example.CampusFlowServer.domain.enrollment.repository;

import com.example.CampusFlowServer.domain.enrollment.entity.CompletedCourse;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompletedCourseRepository extends JpaRepository<CompletedCourse, Long> {

    @EntityGraph(attributePaths = "subject")
    List<CompletedCourse> findByStudentIdAndSubjectIdInAndPassedTrue(
        Long studentId,
        Collection<Long> subjectIds
    );
}
