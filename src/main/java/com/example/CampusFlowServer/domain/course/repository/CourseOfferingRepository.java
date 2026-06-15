package com.example.CampusFlowServer.domain.course.repository;

import com.example.CampusFlowServer.domain.course.entity.CourseOffering;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CourseOfferingRepository
    extends JpaRepository<CourseOffering, Long>, JpaSpecificationExecutor<CourseOffering> {

    @Override
    @EntityGraph(attributePaths = {
        "semester",
        "subject",
        "subject.department",
        "professor",
        "professor.member"
    })
    List<CourseOffering> findAll(Specification<CourseOffering> specification);

    Optional<CourseOffering> findBySemesterIdAndSubjectId(Long semesterId, Long subjectId);

    Optional<CourseOffering> findBySemesterIdAndSubjectIdAndProfessorId(
        Long semesterId,
        Long subjectId,
        Long professorId
    );
}
