package com.example.CampusFlowServer.domain.enrollment.repository;

import com.example.CampusFlowServer.domain.enrollment.entity.WishCourse;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WishCourseRepository extends JpaRepository<WishCourse, Long> {

    @EntityGraph(attributePaths = {
        "semester",
        "student",
        "courseOffering",
        "courseOffering.subject",
        "courseOffering.subject.department",
        "courseOffering.professor",
        "courseOffering.professor.member"
    })
    List<WishCourse> findByStudentIdAndSemesterId(Long studentId, Long semesterId);

    List<WishCourse> findByStudentIdAndCourseOfferingIdIn(
        Long studentId,
        Collection<Long> courseOfferingIds
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
    List<WishCourse> findByCourseOfferingIdInAndAutoApplyTrue(
        Collection<Long> courseOfferingIds
    );

    boolean existsByStudentIdAndSemesterIdAndCourseOfferingId(
        Long studentId,
        Long semesterId,
        Long courseOfferingId
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
    @Query("select w from WishCourse w where w.id = :id")
    Optional<WishCourse> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        select
            w.courseOffering.id as courseOfferingId,
            count(w.id) as wishCount,
            coalesce(sum(case when w.autoApply = true then 1 else 0 end), 0) as autoApplyCount
        from WishCourse w
        where w.courseOffering.id in :courseOfferingIds
        group by w.courseOffering.id
        """)
    List<WishCourseCountProjection> countByCourseOfferingIds(
        @Param("courseOfferingIds") Collection<Long> courseOfferingIds
    );
}
