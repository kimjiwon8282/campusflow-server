package com.example.CampusFlowServer.domain.student.enrollment.service;

import com.example.CampusFlowServer.domain.course.entity.CourseOffering;
import com.example.CampusFlowServer.domain.course.entity.CourseTime;
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
import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import com.example.CampusFlowServer.domain.semester.repository.SemesterRepository;
import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentApplyResponse;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentErrorCode;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutoEnrollmentService {

    private static final List<EnrollmentStatus> ACTIVE_STATUSES = List.of(
        EnrollmentStatus.ENROLLED,
        EnrollmentStatus.WAITING
    );

    private final SemesterRepository semesterRepository;
    private final WishCourseRepository wishCourseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseTimeRepository courseTimeRepository;
    private final CourseAllowedGradeRepository courseAllowedGradeRepository;
    private final CoursePrerequisiteRepository coursePrerequisiteRepository;
    private final CompletedCourseRepository completedCourseRepository;

    @Transactional
    public AutoEnrollmentApplyResponse applyAutoEnrollments(Integer year, SemesterTerm term) {
        if (year == null) {
            throw new StudentEnrollmentException(StudentEnrollmentErrorCode.REQUIRED_YEAR);
        }
        if (term == null) {
            throw new StudentEnrollmentException(StudentEnrollmentErrorCode.REQUIRED_TERM);
        }

        Semester semester = semesterRepository.findByYearAndTerm(year, term)
            .orElseThrow(() -> new StudentEnrollmentException(
                StudentEnrollmentErrorCode.SEMESTER_NOT_FOUND
            ));
        List<WishCourse> targets = wishCourseRepository.findBySemesterIdAndAutoApplyTrueAndResult(
            semester.getId(),
            WishAutoApplyResult.DONE
        );

        int createdCount = 0;
        int skippedCount = 0;
        for (WishCourse wishCourse : targets) {
            if (applyOne(wishCourse)) {
                createdCount++;
            } else {
                skippedCount++;
            }
        }
        return new AutoEnrollmentApplyResponse(targets.size(), createdCount, skippedCount);
    }

    private boolean applyOne(WishCourse wishCourse) {
        StudentProfile student = wishCourse.getStudent();
        CourseOffering offering = wishCourse.getCourseOffering();
        Semester semester = wishCourse.getSemester();

        if (enrollmentRepository.existsByStudentIdAndSemesterIdAndCourseOfferingIdAndStatusIn(
            student.getId(),
            semester.getId(),
            offering.getId(),
            ACTIVE_STATUSES
        )) {
            return false;
        }

        if (!isEligible(student, semester, offering)) {
            // The state may change between auto-apply result checking and actual reflection.
            // This step skips failed items without mutating WishCourse; later phases can add
            // richer retry/result handling.
            return false;
        }

        if (enrolledCount(offering) >= offering.getCapacity()) {
            return false;
        }

        Enrollment enrollment = enrollmentRepository
            .findFirstByStudentIdAndCourseOfferingIdOrderByIdDesc(student.getId(), offering.getId())
            .orElseGet(() -> Enrollment.create(
                student,
                semester,
                offering,
                EnrollmentStatus.ENROLLED,
                EnrollmentSource.AUTO,
                null,
                LocalDateTime.now()
            ));
        if (enrollment.getId() != null) {
            enrollment.reapply(
                EnrollmentStatus.ENROLLED,
                EnrollmentSource.AUTO,
                null,
                LocalDateTime.now()
            );
        }
        enrollmentRepository.saveAndFlush(enrollment);
        return true;
    }

    private boolean isEligible(
        StudentProfile student,
        Semester semester,
        CourseOffering offering
    ) {
        return isGradeAllowed(student, offering)
            && hasPrerequisites(student, offering)
            && currentActiveCredit(student, semester) + offering.getSubject().getCredit()
                <= maxCredit(student, semester)
            && !hasTimeConflict(
                findCourseTimesByOfferingId(List.of(offering.getId()))
                    .getOrDefault(offering.getId(), List.of()),
                findMyActiveCourseTimes(student, semester)
            );
    }

    private boolean isGradeAllowed(StudentProfile student, CourseOffering offering) {
        Set<Integer> allowedGrades = findAllowedGradesByOfferingId(List.of(offering.getId()))
            .getOrDefault(offering.getId(), Set.of());
        return allowedGrades.isEmpty() || allowedGrades.contains(student.getGrade());
    }

    private boolean hasPrerequisites(StudentProfile student, CourseOffering offering) {
        List<CoursePrerequisite> prerequisites = coursePrerequisiteRepository
            .findBySubjectIdInAndActiveTrue(List.of(offering.getSubject().getId()));
        if (prerequisites.isEmpty()) {
            return true;
        }
        Set<Long> completedSubjectIds = findCompletedSubjectIds(student, prerequisites);
        for (CoursePrerequisite prerequisite : prerequisites) {
            if (!completedSubjectIds.contains(prerequisite.getPrerequisiteSubject().getId())) {
                return false;
            }
        }
        return true;
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

    private Map<Long, Set<Integer>> findAllowedGradesByOfferingId(List<Long> offeringIds) {
        Map<Long, Set<Integer>> result = new HashMap<>();
        for (CourseAllowedGrade allowedGrade :
            courseAllowedGradeRepository.findByCourseOfferingIdIn(offeringIds)) {
            result.computeIfAbsent(
                    allowedGrade.getCourseOffering().getId(),
                    ignored -> new TreeSet<>()
                )
                .add(allowedGrade.getGradeLevel());
        }
        return result;
    }

    private Set<Long> findCompletedSubjectIds(
        StudentProfile student,
        List<CoursePrerequisite> prerequisites
    ) {
        List<Long> prerequisiteSubjectIds = prerequisites.stream()
            .map(prerequisite -> prerequisite.getPrerequisiteSubject().getId())
            .distinct()
            .toList();
        if (prerequisiteSubjectIds.isEmpty()) {
            return Set.of();
        }
        return completedCourseRepository
            .findByStudentIdAndSubjectIdInAndPassedTrue(student.getId(), prerequisiteSubjectIds)
            .stream()
            .map(completedCourse -> completedCourse.getSubject().getId())
            .collect(Collectors.toSet());
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
}
