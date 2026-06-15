package com.example.CampusFlowServer.domain.enrollment.repository;

import com.example.CampusFlowServer.domain.enrollment.entity.Enrollment;
import com.example.CampusFlowServer.domain.enrollment.enums.EnrollmentStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @EntityGraph(attributePaths = {
        "semester",
        "student",
        "courseOffering",
        "courseOffering.subject",
        "courseOffering.subject.department",
        "courseOffering.professor",
        "courseOffering.professor.member"
    })
    List<Enrollment> findByStudentIdAndSemesterIdAndStatusIn(
        Long studentId,
        Long semesterId,
        Collection<EnrollmentStatus> statuses
    );

    boolean existsByStudentIdAndSemesterIdAndCourseOfferingIdAndStatusIn(
        Long studentId,
        Long semesterId,
        Long courseOfferingId,
        Collection<EnrollmentStatus> statuses
    );

    @Query("""
        select
            e.courseOffering.id as courseOfferingId,
            e.status as status,
            count(e.id) as count
        from Enrollment e
        where e.courseOffering.id in :courseOfferingIds
          and e.status in :statuses
        group by e.courseOffering.id, e.status
        """)
    List<EnrollmentStatusCountProjection> countByCourseOfferingIdsAndStatuses(
        @Param("courseOfferingIds") Collection<Long> courseOfferingIds,
        @Param("statuses") Collection<EnrollmentStatus> statuses
    );

    @EntityGraph(attributePaths = {"student", "courseOffering"})
    List<Enrollment> findByCourseOfferingIdAndStatusOrderByAppliedAtAscIdAsc(
        Long courseOfferingId,
        EnrollmentStatus status
    );

    @EntityGraph(attributePaths = {
        "semester",
        "student",
        "courseOffering",
        "courseOffering.subject",
        "courseOffering.subject.department",
        "courseOffering.professor",
        "courseOffering.professor.member"
    })
    Optional<Enrollment> findFirstByStudentIdAndCourseOfferingIdAndStatusIn(
        Long studentId,
        Long courseOfferingId,
        Collection<EnrollmentStatus> statuses
    );

    Optional<Enrollment> findFirstByStudentIdAndCourseOfferingIdOrderByIdDesc(
        Long studentId,
        Long courseOfferingId
    );

    @EntityGraph(attributePaths = {
        "semester",
        "student",
        "student.member",
        "courseOffering",
        "courseOffering.subject",
        "courseOffering.subject.department",
        "courseOffering.professor",
        "courseOffering.professor.member"
    })
    @Query("select e from Enrollment e where e.id = :id")
    Optional<Enrollment> findWithDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {
        "courseOffering",
        "courseOffering.subject",
        "courseOffering.subject.department",
        "courseOffering.professor",
        "courseOffering.professor.member"
    })
    List<Enrollment> findByStudentIdAndCourseOfferingIdInAndStatusIn(
        Long studentId,
        Collection<Long> courseOfferingIds,
        Collection<EnrollmentStatus> statuses
    );
}
