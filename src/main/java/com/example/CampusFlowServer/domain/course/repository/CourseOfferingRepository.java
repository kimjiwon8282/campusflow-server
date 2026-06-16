package com.example.CampusFlowServer.domain.course.repository;

import com.example.CampusFlowServer.domain.course.entity.CourseOffering;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseOfferingRepository
    extends JpaRepository<CourseOffering, Long>, JpaSpecificationExecutor<CourseOffering> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select co from CourseOffering co where co.id = :id")
    Optional<CourseOffering> findByIdForUpdate(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {
        "semester",
        "subject",
        "subject.department",
        "professor",
        "professor.member"
    })
    List<CourseOffering> findAll(Specification<CourseOffering> specification);

    @EntityGraph(attributePaths = {
        "semester",
        "subject"
    })
    List<CourseOffering> findBySemesterId(Long semesterId);

    Optional<CourseOffering> findBySemesterIdAndSubjectId(Long semesterId, Long subjectId);

    Optional<CourseOffering> findBySemesterIdAndSubjectIdAndProfessorId(
        Long semesterId,
        Long subjectId,
        Long professorId
    );
}
