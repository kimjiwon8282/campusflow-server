package com.example.CampusFlowServer.domain.student.enrollment.service;

import com.example.CampusFlowServer.domain.course.entity.CourseOffering;
import com.example.CampusFlowServer.domain.course.entity.CourseTime;
import com.example.CampusFlowServer.domain.course.repository.CourseOfferingRepository;
import com.example.CampusFlowServer.domain.course.repository.CourseTimeRepository;
import com.example.CampusFlowServer.domain.enrollment.entity.CourseAllowedGrade;
import com.example.CampusFlowServer.domain.enrollment.entity.CoursePrerequisite;
import com.example.CampusFlowServer.domain.enrollment.entity.Enrollment;
import com.example.CampusFlowServer.domain.enrollment.entity.WishCourse;
import com.example.CampusFlowServer.domain.enrollment.enums.EnrollmentSource;
import com.example.CampusFlowServer.domain.enrollment.enums.EnrollmentStatus;
import com.example.CampusFlowServer.domain.enrollment.enums.WishAutoApplyResult;
import com.example.CampusFlowServer.domain.enrollment.repository.CompletedCourseRepository;
import com.example.CampusFlowServer.domain.enrollment.repository.CourseAllowedGradeRepository;
import com.example.CampusFlowServer.domain.enrollment.repository.CoursePrerequisiteRepository;
import com.example.CampusFlowServer.domain.enrollment.repository.EnrollmentRepository;
import com.example.CampusFlowServer.domain.enrollment.repository.EnrollmentStatusCountProjection;
import com.example.CampusFlowServer.domain.enrollment.repository.WishCourseRepository;
import com.example.CampusFlowServer.domain.member.entity.StudentProfile;
import com.example.CampusFlowServer.domain.semester.entity.Semester;
import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentApplyOneResult;
import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentApplyOneStatus;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentErrorCode;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutoEnrollmentCommandService {

    private static final List<EnrollmentStatus> ACTIVE_STATUSES = List.of(
        EnrollmentStatus.ENROLLED,
        EnrollmentStatus.WAITING
    );

    private final CourseOfferingRepository courseOfferingRepository;
    private final WishCourseRepository wishCourseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseTimeRepository courseTimeRepository;
    private final CourseAllowedGradeRepository courseAllowedGradeRepository;
    private final CoursePrerequisiteRepository coursePrerequisiteRepository;
    private final CompletedCourseRepository completedCourseRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AutoEnrollmentApplyOneResult applyOne(Long wishCourseId, Long courseOfferingId) {
        CourseOffering offering = courseOfferingRepository.findByIdForUpdate(courseOfferingId)
            .orElseThrow(() -> new StudentEnrollmentException(
                StudentEnrollmentErrorCode.COURSE_OFFERING_NOT_FOUND
            ));

        WishCourse wishCourse = wishCourseRepository.findByIdWithDetails(wishCourseId)
            .orElse(null);
        if (wishCourse == null) {
            return skipped(
                wishCourseId,
                AutoEnrollmentApplyOneStatus.SKIPPED_WISH_COURSE_NOT_FOUND,
                "Wish course was not found."
            );
        }
        if (!offering.getId().equals(wishCourse.getCourseOffering().getId())) {
            return skipped(
                wishCourseId,
                AutoEnrollmentApplyOneStatus.SKIPPED_COURSE_OFFERING_MISMATCH,
                "Course offering does not match the wish course."
            );
        }
        if (!wishCourse.isAutoApply()
            || !WishAutoApplyResult.DONE.equals(wishCourse.getResult())) {
            return skipped(
                wishCourseId,
                AutoEnrollmentApplyOneStatus.SKIPPED_NOT_DONE,
                "Wish course is not a confirmed auto-apply target."
            );
        }

        StudentProfile student = wishCourse.getStudent();
        Semester semester = wishCourse.getSemester();
        if (enrollmentRepository.existsByStudentIdAndSemesterIdAndCourseOfferingIdAndStatusIn(
            student.getId(),
            semester.getId(),
            offering.getId(),
            ACTIVE_STATUSES
        )) {
            return skipped(
                wishCourseId,
                AutoEnrollmentApplyOneStatus.SKIPPED_ALREADY_ACTIVE,
                "An active enrollment already exists."
            );
        }

        AutoEnrollmentApplyOneResult eligibilityFailure = validateEligibility(
            wishCourseId,
            student,
            semester,
            offering
        );
        if (eligibilityFailure != null) {
            return eligibilityFailure;
        }
        if (enrolledCount(offering) >= offering.getCapacity()) {
            return skipped(
                wishCourseId,
                AutoEnrollmentApplyOneStatus.SKIPPED_OVER_CAPACITY,
                "Course capacity has been reached."
            );
        }

        Enrollment enrollment = enrollmentRepository
            .findFirstByStudentIdAndCourseOfferingIdOrderByIdDesc(student.getId(), offering.getId())
            .orElse(null);
        boolean reactivated = enrollment != null;
        if (enrollment == null) {
            enrollment = Enrollment.create(
                student,
                semester,
                offering,
                EnrollmentStatus.ENROLLED,
                EnrollmentSource.AUTO,
                null,
                LocalDateTime.now()
            );
        } else {
            enrollment.reapply(
                EnrollmentStatus.ENROLLED,
                EnrollmentSource.AUTO,
                null,
                LocalDateTime.now()
            );
        }
        Enrollment saved = enrollmentRepository.saveAndFlush(enrollment);

        return new AutoEnrollmentApplyOneResult(
            wishCourseId,
            saved.getId(),
            reactivated
                ? AutoEnrollmentApplyOneStatus.REACTIVATED
                : AutoEnrollmentApplyOneStatus.APPLIED,
            reactivated
                ? "Cancelled enrollment was reactivated as an auto enrollment."
                : "Auto enrollment was applied."
        );
    }

    private AutoEnrollmentApplyOneResult validateEligibility(
        Long wishCourseId,
        StudentProfile student,
        Semester semester,
        CourseOffering offering
    ) {
        Set<Integer> allowedGrades = findAllowedGrades(offering);
        if (!allowedGrades.isEmpty() && !allowedGrades.contains(student.getGrade())) {
            return skipped(
                wishCourseId,
                AutoEnrollmentApplyOneStatus.SKIPPED_GRADE_RESTRICTED,
                "Student grade is not allowed for this course."
            );
        }
        if (!hasPrerequisites(student, offering)) {
            return skipped(
                wishCourseId,
                AutoEnrollmentApplyOneStatus.SKIPPED_PREREQUISITE_REQUIRED,
                "Required prerequisite has not been completed."
            );
        }
        if (currentActiveCredit(student, semester) + offering.getSubject().getCredit()
            > maxCredit(student, semester)) {
            return skipped(
                wishCourseId,
                AutoEnrollmentApplyOneStatus.SKIPPED_CREDIT_LIMIT,
                "Student credit limit would be exceeded."
            );
        }
        if (hasTimeConflict(
            findCourseTimesByOfferingId(List.of(offering.getId()))
                .getOrDefault(offering.getId(), List.of()),
            findMyActiveCourseTimes(student, semester)
        )) {
            return skipped(
                wishCourseId,
                AutoEnrollmentApplyOneStatus.SKIPPED_TIME_CONFLICT,
                "Course time conflicts with an active enrollment."
            );
        }
        return null;
    }

    private Set<Integer> findAllowedGrades(CourseOffering offering) {
        Set<Integer> result = new TreeSet<>();
        for (CourseAllowedGrade allowedGrade :
            courseAllowedGradeRepository.findByCourseOfferingIdIn(List.of(offering.getId()))) {
            result.add(allowedGrade.getGradeLevel());
        }
        return result;
    }

    private boolean hasPrerequisites(StudentProfile student, CourseOffering offering) {
        List<CoursePrerequisite> prerequisites = coursePrerequisiteRepository
            .findBySubjectIdInAndActiveTrue(List.of(offering.getSubject().getId()));
        if (prerequisites.isEmpty()) {
            return true;
        }
        List<Long> prerequisiteSubjectIds = prerequisites.stream()
            .map(prerequisite -> prerequisite.getPrerequisiteSubject().getId())
            .distinct()
            .toList();
        Set<Long> completedSubjectIds = completedCourseRepository
            .findByStudentIdAndSubjectIdInAndPassedTrue(student.getId(), prerequisiteSubjectIds)
            .stream()
            .map(completedCourse -> completedCourse.getSubject().getId())
            .collect(Collectors.toSet());
        return completedSubjectIds.containsAll(prerequisiteSubjectIds);
    }

    private int maxCredit(StudentProfile student, Semester semester) {
        return student.getMaxCredit() == null ? semester.getMaxCredit() : student.getMaxCredit();
    }

    private int currentActiveCredit(StudentProfile student, Semester semester) {
        return enrollmentRepository.findByStudentIdAndSemesterIdAndStatusIn(
                student.getId(),
                semester.getId(),
                ACTIVE_STATUSES
            )
            .stream()
            .mapToInt(enrollment -> enrollment.getCourseOffering().getSubject().getCredit())
            .sum();
    }

    private long enrolledCount(CourseOffering offering) {
        return enrollmentRepository.countByCourseOfferingIdsAndStatuses(
                List.of(offering.getId()),
                List.of(EnrollmentStatus.ENROLLED)
            )
            .stream()
            .filter(projection -> EnrollmentStatus.ENROLLED.equals(projection.getStatus()))
            .mapToLong(EnrollmentStatusCountProjection::getCount)
            .sum();
    }

    private List<CourseTime> findMyActiveCourseTimes(StudentProfile student, Semester semester) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndSemesterIdAndStatusIn(
            student.getId(),
            semester.getId(),
            ACTIVE_STATUSES
        );
        if (enrollments.isEmpty()) {
            return List.of();
        }
        List<Long> offeringIds = enrollments.stream()
            .map(enrollment -> enrollment.getCourseOffering().getId())
            .toList();
        return findCourseTimesByOfferingId(offeringIds).values().stream()
            .flatMap(Collection::stream)
            .toList();
    }

    private Map<Long, List<CourseTime>> findCourseTimesByOfferingId(List<Long> offeringIds) {
        if (offeringIds.isEmpty()) {
            return Map.of();
        }
        return courseTimeRepository
            .findByCourseOfferingIdInOrderByCourseOfferingIdAscDayOfWeekAscStartPeriodAsc(offeringIds)
            .stream()
            .collect(Collectors.groupingBy(time -> time.getCourseOffering().getId()));
    }

    private boolean hasTimeConflict(List<CourseTime> candidateTimes, List<CourseTime> activeTimes) {
        for (CourseTime candidate : candidateTimes) {
            for (CourseTime active : activeTimes) {
                if (candidate.getCourseOffering().getId().equals(active.getCourseOffering().getId())) {
                    continue;
                }
                if (candidate.getDayOfWeek().equals(active.getDayOfWeek())
                    && candidate.getStartPeriod() <= active.getEndPeriod()
                    && active.getStartPeriod() <= candidate.getEndPeriod()) {
                    return true;
                }
            }
        }
        return false;
    }

    private AutoEnrollmentApplyOneResult skipped(
        Long wishCourseId,
        AutoEnrollmentApplyOneStatus status,
        String message
    ) {
        return new AutoEnrollmentApplyOneResult(wishCourseId, null, status, message);
    }
}
