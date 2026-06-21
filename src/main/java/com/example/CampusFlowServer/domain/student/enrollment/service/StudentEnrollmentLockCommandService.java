package com.example.CampusFlowServer.domain.student.enrollment.service;

import com.example.CampusFlowServer.domain.course.entity.CourseOffering;
import com.example.CampusFlowServer.domain.course.entity.CourseTime;
import com.example.CampusFlowServer.domain.course.repository.CourseOfferingRepository;
import com.example.CampusFlowServer.domain.course.repository.CourseTimeRepository;
import com.example.CampusFlowServer.domain.enrollment.entity.Enrollment;
import com.example.CampusFlowServer.domain.enrollment.enums.EnrollmentSource;
import com.example.CampusFlowServer.domain.enrollment.enums.EnrollmentStatus;
import com.example.CampusFlowServer.domain.enrollment.repository.EnrollmentRepository;
import com.example.CampusFlowServer.domain.enrollment.repository.EnrollmentStatusCountProjection;
import com.example.CampusFlowServer.domain.member.entity.StudentProfile;
import com.example.CampusFlowServer.domain.member.repository.StudentProfileRepository;
import com.example.CampusFlowServer.domain.semester.entity.Semester;
import com.example.CampusFlowServer.domain.semester.entity.SemesterSchedule;
import com.example.CampusFlowServer.domain.semester.enums.SemesterScheduleType;
import com.example.CampusFlowServer.domain.semester.repository.SemesterScheduleRepository;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentErrorCode;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentEnrollmentLockCommandService {

    private static final List<EnrollmentStatus> ACTIVE_STATUSES = List.of(
        EnrollmentStatus.ENROLLED,
        EnrollmentStatus.WAITING
    );

    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseTimeRepository courseTimeRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SemesterScheduleRepository semesterScheduleRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EnrollmentApplyCoreResult applyWithLock(EnrollmentPreValidationResult preValidation) {
        long commandStartedAt = System.nanoTime();
        long lockStartedAt = System.nanoTime();
        CourseOffering offering = findCourseOfferingForUpdate(preValidation.courseOfferingId());
        logDebugElapsed("student enrollment CourseOffering lock acquisition", lockStartedAt);

        validateEnrollmentPeriod(offering.getSemester());
        StudentProfile student = findStudent(preValidation.studentId());
        Semester semester = offering.getSemester();
        List<Enrollment> activeEnrollments = findActiveEnrollments(student, semester);
        validateNotDuplicate(activeEnrollments, offering);
        validateCreditLimit(student, semester, offering, activeEnrollments);
        validateNoTimeConflict(offering, activeEnrollments);

        long enrolledCount = findEnrollmentStatsByOfferingId(List.of(offering.getId()))
            .getOrDefault(offering.getId(), EnrollmentStats.EMPTY)
            .enrolledCount();
        EnrollmentStatus status = enrolledCount < offering.getCapacity()
            ? EnrollmentStatus.ENROLLED
            : EnrollmentStatus.WAITING;
        Enrollment enrollment = saveOrReactivateEnrollment(student, semester, offering, status);

        logDebugElapsed("student enrollment command transaction", commandStartedAt);
        return new EnrollmentApplyCoreResult(
            enrollment.getId(),
            offering.getId(),
            offering.getSubject().getCode(),
            offering.getSubject().getName(),
            status,
            EnrollmentSource.MANUAL
        );
    }

    private CourseOffering findCourseOfferingForUpdate(Long courseOfferingId) {
        return courseOfferingRepository.findByIdForUpdate(courseOfferingId)
            .orElseThrow(() -> new StudentEnrollmentException(
                StudentEnrollmentErrorCode.COURSE_OFFERING_NOT_FOUND
            ));
    }

    private StudentProfile findStudent(Long studentId) {
        return studentProfileRepository.findById(studentId)
            .orElseThrow(() -> new StudentEnrollmentException(
                StudentEnrollmentErrorCode.STUDENT_PROFILE_NOT_FOUND
            ));
    }

    private void validateEnrollmentPeriod(Semester semester) {
        SemesterSchedule schedule = semesterScheduleRepository
            .findBySemesterIdAndType(semester.getId(), SemesterScheduleType.ENROLLMENT)
            .orElseThrow(() -> new StudentEnrollmentException(
                StudentEnrollmentErrorCode.ENROLLMENT_PERIOD_CLOSED
            ));
        if (!isScheduleOpen(schedule)) {
            throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.ENROLLMENT_PERIOD_CLOSED
            );
        }
    }

    private boolean isScheduleOpen(SemesterSchedule schedule) {
        if (schedule.isAlwaysOpen()) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        return schedule.getStartAt() != null
            && schedule.getEndAt() != null
            && !now.isBefore(schedule.getStartAt())
            && !now.isAfter(schedule.getEndAt());
    }

    private List<Enrollment> findActiveEnrollments(StudentProfile student, Semester semester) {
        return enrollmentRepository.findByStudentIdAndSemesterIdAndStatusIn(
            student.getId(),
            semester.getId(),
            ACTIVE_STATUSES
        );
    }

    private void validateNotDuplicate(
        List<Enrollment> activeEnrollments,
        CourseOffering offering
    ) {
        boolean duplicate = activeEnrollments.stream()
            .anyMatch(enrollment -> enrollment.getCourseOffering().getId().equals(offering.getId()));
        if (duplicate) {
            throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.DUPLICATE_ENROLLMENT
            );
        }
    }

    private void validateCreditLimit(
        StudentProfile student,
        Semester semester,
        CourseOffering offering,
        List<Enrollment> activeEnrollments
    ) {
        int currentCredit = activeEnrollments.stream()
            .mapToInt(enrollment -> enrollment.getCourseOffering().getSubject().getCredit())
            .sum();
        int maxCredit = student.getMaxCredit() == null ? semester.getMaxCredit() : student.getMaxCredit();
        if (currentCredit + offering.getSubject().getCredit() > maxCredit) {
            throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.CREDIT_LIMIT_EXCEEDED
            );
        }
    }

    private void validateNoTimeConflict(
        CourseOffering offering,
        List<Enrollment> activeEnrollments
    ) {
        List<Long> offeringIds = Stream.concat(
                Stream.of(offering.getId()),
                activeEnrollments.stream()
                    .map(enrollment -> enrollment.getCourseOffering().getId())
            )
            .distinct()
            .toList();
        Map<Long, List<CourseTime>> timesByOfferingId = findCourseTimesByOfferingId(offeringIds);
        List<CourseTime> candidateTimes = timesByOfferingId.getOrDefault(offering.getId(), List.of());
        List<CourseTime> activeTimes = activeEnrollments.stream()
            .map(enrollment -> timesByOfferingId.getOrDefault(
                enrollment.getCourseOffering().getId(),
                List.of()
            ))
            .flatMap(Collection::stream)
            .toList();
        if (hasTimeConflict(candidateTimes, activeTimes)) {
            throw new StudentEnrollmentException(StudentEnrollmentErrorCode.TIME_CONFLICT);
        }
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

    private boolean hasTimeConflict(List<CourseTime> candidateTimes, List<CourseTime> myActiveTimes) {
        for (CourseTime candidate : candidateTimes) {
            for (CourseTime active : myActiveTimes) {
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

    private Map<Long, EnrollmentStats> findEnrollmentStatsByOfferingId(List<Long> offeringIds) {
        if (offeringIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, EnrollmentStats> statsByOfferingId = new HashMap<>();
        for (EnrollmentStatusCountProjection projection :
            enrollmentRepository.countByCourseOfferingIdsAndStatuses(offeringIds, ACTIVE_STATUSES)) {
            EnrollmentStats current = statsByOfferingId.getOrDefault(
                projection.getCourseOfferingId(),
                EnrollmentStats.EMPTY
            );
            statsByOfferingId.put(
                projection.getCourseOfferingId(),
                current.with(projection.getStatus(), projection.getCount())
            );
        }
        return statsByOfferingId;
    }

    private Enrollment saveOrReactivateEnrollment(
        StudentProfile student,
        Semester semester,
        CourseOffering offering,
        EnrollmentStatus status
    ) {
        Enrollment enrollment = enrollmentRepository
            .findFirstByStudentIdAndCourseOfferingIdOrderByIdDesc(student.getId(), offering.getId())
            .orElseGet(() -> Enrollment.create(
                student,
                semester,
                offering,
                status,
                EnrollmentSource.MANUAL,
                null,
                LocalDateTime.now()
            ));
        if (enrollment.getId() != null) {
            enrollment.reapply(status, EnrollmentSource.MANUAL, null, LocalDateTime.now());
        }
        return enrollmentRepository.saveAndFlush(enrollment);
    }

    private void logDebugElapsed(String label, long startedAt) {
        if (log.isDebugEnabled()) {
            log.debug("{} took {} ms", label, elapsedMillis(startedAt));
        }
    }

    private double elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000.0;
    }

    private record EnrollmentStats(Long enrolledCount, Long waitingCount) {

        private static final EnrollmentStats EMPTY = new EnrollmentStats(0L, 0L);

        private EnrollmentStats with(EnrollmentStatus status, Long count) {
            if (EnrollmentStatus.ENROLLED.equals(status)) {
                return new EnrollmentStats(count, waitingCount);
            }
            if (EnrollmentStatus.WAITING.equals(status)) {
                return new EnrollmentStats(enrolledCount, count);
            }
            return this;
        }
    }
}
