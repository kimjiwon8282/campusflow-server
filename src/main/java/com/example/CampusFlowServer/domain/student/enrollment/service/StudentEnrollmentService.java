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
import com.example.CampusFlowServer.domain.member.repository.StudentProfileRepository;
import com.example.CampusFlowServer.domain.semester.entity.Semester;
import com.example.CampusFlowServer.domain.semester.entity.SemesterSchedule;
import com.example.CampusFlowServer.domain.semester.enums.SemesterScheduleType;
import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import com.example.CampusFlowServer.domain.semester.repository.SemesterRepository;
import com.example.CampusFlowServer.domain.semester.repository.SemesterScheduleRepository;
import com.example.CampusFlowServer.domain.student.catalog.dto.StudentCatalogCourseSearchRequest;
import com.example.CampusFlowServer.domain.student.catalog.specification.CourseOfferingCatalogSpecification;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrolledCourseResponse;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentApplyRequest;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentApplyResponse;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentCancelResponse;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentCourseResponse;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentCourseStatus;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentListResponse;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentSemesterRequest;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentSummaryResponse;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentManualRequiredCourseResponse;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentWaitingCourseResponse;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentErrorCode;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentEnrollmentService {

    private static final String CONDITION_MODE = "condition";
    private static final String DIRECT_MODE = "direct";
    private static final List<EnrollmentStatus> ACTIVE_STATUSES = List.of(
        EnrollmentStatus.ENROLLED,
        EnrollmentStatus.WAITING
    );

    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseTimeRepository courseTimeRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final WishCourseRepository wishCourseRepository;
    private final CourseAllowedGradeRepository courseAllowedGradeRepository;
    private final CoursePrerequisiteRepository coursePrerequisiteRepository;
    private final CompletedCourseRepository completedCourseRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SemesterRepository semesterRepository;
    private final SemesterScheduleRepository semesterScheduleRepository;

    @Transactional
    public StudentEnrollmentApplyResponse apply(
        Long memberId,
        StudentEnrollmentApplyRequest request
    ) {
        if (request == null || request.courseOfferingId() == null) {
            throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.REQUIRED_COURSE_OFFERING
            );
        }

        CourseOffering offering = findCourseOfferingForUpdate(request.courseOfferingId());
        StudentProfile student = findStudent(memberId);
        Semester semester = offering.getSemester();

        validateEnrollmentPeriod(semester);
        validateNotDuplicate(student, semester, offering);
        validateGradeAllowed(student, offering);
        validatePrerequisites(student, offering);
        validateCreditLimit(student, semester, offering);
        validateNoTimeConflict(student, semester, offering);

        long enrolledCount = findEnrollmentStatsByOfferingId(List.of(offering.getId()))
            .getOrDefault(offering.getId(), EnrollmentStats.EMPTY)
            .enrolledCount();
        EnrollmentStatus status = enrolledCount < offering.getCapacity()
            ? EnrollmentStatus.ENROLLED
            : EnrollmentStatus.WAITING;
        Enrollment enrollment = saveOrReactivateEnrollment(student, semester, offering, status);
        Integer waitNo = EnrollmentStatus.WAITING.equals(status) ? waitNo(enrollment) : null;

        return new StudentEnrollmentApplyResponse(
            enrollment.getId(),
            offering.getId(),
            offering.getSubject().getCode(),
            offering.getSubject().getName(),
            status.name(),
            EnrollmentSource.MANUAL.name(),
            waitNo,
            EnrollmentStatus.ENROLLED.equals(status)
                ? "수강신청이 완료되었습니다."
                : "정원이 마감되어 대기 신청되었습니다."
        );
    }

    @Transactional
    public StudentEnrollmentCancelResponse cancel(Long memberId, Long enrollmentId) {
        StudentProfile student = findStudent(memberId);
        Enrollment enrollment = enrollmentRepository.findWithDetailsById(enrollmentId)
            .orElseThrow(() -> new StudentEnrollmentException(
                StudentEnrollmentErrorCode.ENROLLMENT_NOT_FOUND
            ));
        validateEnrollmentOwner(enrollment, student);
        CourseOffering offering = findCourseOfferingForUpdate(enrollment.getCourseOffering().getId());
        validateEnrollmentPeriod(offering.getSemester());

        if (EnrollmentStatus.CANCELLED.equals(enrollment.getStatus())) {
            throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.ENROLLMENT_NOT_CANCELABLE
            );
        }
        if (!ACTIVE_STATUSES.contains(enrollment.getStatus())) {
            throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.ENROLLMENT_NOT_CANCELABLE
            );
        }

        boolean wasEnrolled = EnrollmentStatus.ENROLLED.equals(enrollment.getStatus());
        enrollment.cancel(LocalDateTime.now());
        Long promotedEnrollmentId = wasEnrolled
            ? promoteFirstWaitingIfEligible(offering)
            : null;

        return new StudentEnrollmentCancelResponse(
            enrollment.getId(),
            enrollment.getCourseOffering().getId(),
            enrollment.getStatus().name(),
            promotedEnrollmentId,
            promotedEnrollmentId == null
                ? "수강신청이 취소되었습니다."
                : "수강신청이 취소되었고 대기 1순위가 수강 확정되었습니다."
        );
    }

    public StudentEnrollmentListResponse getMyEnrollments(
        Long memberId,
        StudentEnrollmentSemesterRequest request
    ) {
        validateRequired(request.year(), request.term());
        StudentProfile student = findStudent(memberId);
        Semester semester = findSemester(request.year(), request.term());

        List<Enrollment> enrollments = enrollmentRepository
            .findByStudentIdAndSemesterIdAndStatusIn(
                student.getId(),
                semester.getId(),
                ACTIVE_STATUSES
            )
            .stream()
            .sorted(enrollmentComparator())
            .toList();
        if (enrollments.isEmpty()) {
            int maxCredit = maxCredit(student, semester);
            return new StudentEnrollmentListResponse(
                new StudentEnrollmentSummaryResponse(semester.getName(), 0, 0, maxCredit, maxCredit, 0, 0),
                List.of(),
                List.of()
            );
        }

        List<Long> offeringIds = enrollments.stream()
            .map(enrollment -> enrollment.getCourseOffering().getId())
            .distinct()
            .toList();
        Map<Long, List<CourseTime>> timesByOfferingId = findCourseTimesByOfferingId(offeringIds);
        Map<Long, EnrollmentStats> statsByOfferingId = findEnrollmentStatsByOfferingId(offeringIds);

        List<StudentEnrolledCourseResponse> enrolledCourses = enrollments.stream()
            .filter(enrollment -> EnrollmentStatus.ENROLLED.equals(enrollment.getStatus()))
            .map(enrollment -> toEnrolledCourseResponse(
                enrollment,
                timesByOfferingId.getOrDefault(enrollment.getCourseOffering().getId(), List.of())
            ))
            .toList();
        List<StudentWaitingCourseResponse> waitingCourses = enrollments.stream()
            .filter(enrollment -> EnrollmentStatus.WAITING.equals(enrollment.getStatus()))
            .map(enrollment -> toWaitingCourseResponse(
                enrollment,
                timesByOfferingId.getOrDefault(enrollment.getCourseOffering().getId(), List.of()),
                statsByOfferingId.getOrDefault(enrollment.getCourseOffering().getId(), EnrollmentStats.EMPTY),
                waitNo(enrollment)
            ))
            .toList();

        int currentCredit = enrolledCourses.stream()
            .mapToInt(StudentEnrolledCourseResponse::credit)
            .sum();
        int activeCredit = enrollments.stream()
            .mapToInt(enrollment -> enrollment.getCourseOffering().getSubject().getCredit())
            .sum();
        int maxCredit = maxCredit(student, semester);
        return new StudentEnrollmentListResponse(
            new StudentEnrollmentSummaryResponse(
                semester.getName(),
                currentCredit,
                activeCredit,
                maxCredit,
                Math.max(0, maxCredit - activeCredit),
                enrolledCourses.size(),
                waitingCourses.size()
            ),
            enrolledCourses,
            waitingCourses
        );
    }

    public List<StudentManualRequiredCourseResponse> getManualRequiredCourses(
        Long memberId,
        StudentEnrollmentSemesterRequest request
    ) {
        validateRequired(request.year(), request.term());
        StudentProfile student = findStudent(memberId);
        Semester semester = findSemester(request.year(), request.term());

        List<WishCourse> wishCourses = wishCourseRepository
            .findByStudentIdAndSemesterId(student.getId(), semester.getId())
            .stream()
            .filter(wishCourse -> !WishAutoApplyResult.DONE.equals(wishCourse.getResult()))
            .sorted(wishCourseComparator())
            .toList();
        if (wishCourses.isEmpty()) {
            return List.of();
        }

        List<Long> offeringIds = wishCourses.stream()
            .map(wishCourse -> wishCourse.getCourseOffering().getId())
            .toList();
        Map<Long, List<CourseTime>> timesByOfferingId = findCourseTimesByOfferingId(offeringIds);
        Set<Long> activeEnrollmentOfferingIds = enrollmentRepository
            .findByStudentIdAndCourseOfferingIdInAndStatusIn(
                student.getId(),
                offeringIds,
                ACTIVE_STATUSES
            )
            .stream()
            .map(enrollment -> enrollment.getCourseOffering().getId())
            .collect(Collectors.toSet());

        return wishCourses.stream()
            .map(wishCourse -> toManualRequiredResponse(
                wishCourse,
                timesByOfferingId.getOrDefault(wishCourse.getCourseOffering().getId(), List.of()),
                !activeEnrollmentOfferingIds.contains(wishCourse.getCourseOffering().getId())
            ))
            .toList();
    }

    public List<StudentEnrollmentCourseResponse> searchCourses(
        Long memberId,
        StudentCatalogCourseSearchRequest request
    ) {
        validateRequired(request.year(), request.term());
        StudentProfile student = findStudent(memberId);
        Semester semester = findSemester(request.year(), request.term());

        String mode = normalizeMode(request.mode());
        Specification<CourseOffering> specification = switch (mode) {
            case CONDITION_MODE -> CourseOfferingCatalogSpecification.byCondition(
                request.year(),
                request.term(),
                normalizeCondition(request.collegeName()),
                normalizeCondition(request.departmentName()),
                normalizeCondition(request.category())
            );
            case DIRECT_MODE -> CourseOfferingCatalogSpecification.byDirect(
                request.year(),
                request.term(),
                normalizeCondition(request.subjectName()),
                normalizeCondition(request.professorName()),
                normalizeCondition(request.departmentName())
            );
            default -> throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.INVALID_SEARCH_MODE
            );
        };

        List<CourseOffering> offerings = courseOfferingRepository.findAll(specification)
            .stream()
            .sorted(courseOfferingComparator())
            .toList();
        if (offerings.isEmpty()) {
            return List.of();
        }

        List<Long> offeringIds = offerings.stream().map(CourseOffering::getId).toList();
        List<Long> subjectIds = offerings.stream()
            .map(offering -> offering.getSubject().getId())
            .distinct()
            .toList();
        Map<Long, List<CourseTime>> timesByOfferingId = findCourseTimesByOfferingId(offeringIds);
        Map<Long, EnrollmentStats> statsByOfferingId = findEnrollmentStatsByOfferingId(offeringIds);
        Map<Long, Enrollment> myEnrollmentsByOfferingId = enrollmentRepository
            .findByStudentIdAndCourseOfferingIdInAndStatusIn(
                student.getId(),
                offeringIds,
                ACTIVE_STATUSES
            )
            .stream()
            .collect(Collectors.toMap(
                enrollment -> enrollment.getCourseOffering().getId(),
                Function.identity()
            ));
        Map<Long, Set<Integer>> allowedGradesByOfferingId = findAllowedGradesByOfferingId(offeringIds);
        List<CoursePrerequisite> prerequisites = coursePrerequisiteRepository
            .findBySubjectIdInAndActiveTrue(subjectIds);
        Map<Long, List<CoursePrerequisite>> prerequisitesBySubjectId = prerequisites.stream()
            .collect(Collectors.groupingBy(prerequisite -> prerequisite.getSubject().getId()));
        Set<Long> completedSubjectIds = findCompletedSubjectIds(student, prerequisites);
        List<CourseTime> myActiveTimes = findMyActiveCourseTimes(student, semester);
        int currentCredit = currentActiveCredit(student, semester);
        int maxCredit = maxCredit(student, semester);
        boolean enrollmentOpen = isEnrollmentOpen(semester);

        return offerings.stream()
            .map(offering -> toSearchResponse(
                offering,
                timesByOfferingId.getOrDefault(offering.getId(), List.of()),
                statsByOfferingId.getOrDefault(offering.getId(), EnrollmentStats.EMPTY),
                myEnrollmentsByOfferingId.get(offering.getId()),
                allowedGradesByOfferingId.getOrDefault(offering.getId(), Set.of()),
                prerequisitesBySubjectId.getOrDefault(offering.getSubject().getId(), List.of()),
                completedSubjectIds,
                myActiveTimes,
                student.getGrade(),
                currentCredit,
                maxCredit,
                enrollmentOpen
            ))
            .toList();
    }

    private void validateRequired(Integer year, SemesterTerm term) {
        if (year == null) {
            throw new StudentEnrollmentException(StudentEnrollmentErrorCode.REQUIRED_YEAR);
        }
        if (term == null) {
            throw new StudentEnrollmentException(StudentEnrollmentErrorCode.REQUIRED_TERM);
        }
    }

    private StudentProfile findStudent(Long memberId) {
        return studentProfileRepository.findByMemberId(memberId)
            .orElseThrow(() -> new StudentEnrollmentException(
                StudentEnrollmentErrorCode.STUDENT_PROFILE_NOT_FOUND
            ));
    }

    private CourseOffering findCourseOffering(Long courseOfferingId) {
        return courseOfferingRepository.findById(courseOfferingId)
            .orElseThrow(() -> new StudentEnrollmentException(
                StudentEnrollmentErrorCode.COURSE_OFFERING_NOT_FOUND
            ));
    }

    private CourseOffering findCourseOfferingForUpdate(Long courseOfferingId) {
        return courseOfferingRepository.findByIdForUpdate(courseOfferingId)
            .orElseThrow(() -> new StudentEnrollmentException(
                StudentEnrollmentErrorCode.COURSE_OFFERING_NOT_FOUND
            ));
    }

    private Semester findSemester(Integer year, SemesterTerm term) {
        return semesterRepository.findByYearAndTerm(year, term)
            .orElseThrow(() -> new StudentEnrollmentException(
                StudentEnrollmentErrorCode.SEMESTER_NOT_FOUND
            ));
    }

    private int maxCredit(StudentProfile student, Semester semester) {
        return student.getMaxCredit() == null ? semester.getMaxCredit() : student.getMaxCredit();
    }

    private int currentActiveCredit(StudentProfile student, Semester semester) {
        return currentActiveCredit(student, semester, null);
    }

    private int currentActiveCredit(
        StudentProfile student,
        Semester semester,
        Long excludedEnrollmentId
    ) {
        return enrollmentRepository.findByStudentIdAndSemesterIdAndStatusIn(
                student.getId(),
                semester.getId(),
                ACTIVE_STATUSES
            )
            .stream()
            .filter(enrollment -> excludedEnrollmentId == null
                || !excludedEnrollmentId.equals(enrollment.getId()))
            .mapToInt(enrollment -> enrollment.getCourseOffering().getSubject().getCredit())
            .sum();
    }

    private boolean isEnrollmentOpen(Semester semester) {
        return semesterScheduleRepository
            .findBySemesterIdAndType(semester.getId(), SemesterScheduleType.ENROLLMENT)
            .map(this::isScheduleOpen)
            .orElse(true);
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

    private void validateNotDuplicate(
        StudentProfile student,
        Semester semester,
        CourseOffering offering
    ) {
        if (enrollmentRepository.existsByStudentIdAndSemesterIdAndCourseOfferingIdAndStatusIn(
            student.getId(),
            semester.getId(),
            offering.getId(),
            ACTIVE_STATUSES
        )) {
            throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.DUPLICATE_ENROLLMENT
            );
        }
    }

    private void validateEnrollmentOwner(Enrollment enrollment, StudentProfile student) {
        if (!enrollment.getStudent().getId().equals(student.getId())) {
            throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.ENROLLMENT_ACCESS_DENIED
            );
        }
    }

    private void validateGradeAllowed(StudentProfile student, CourseOffering offering) {
        Set<Integer> allowedGrades = findAllowedGradesByOfferingId(List.of(offering.getId()))
            .getOrDefault(offering.getId(), Set.of());
        if (!allowedGrades.isEmpty() && !allowedGrades.contains(student.getGrade())) {
            throw new StudentEnrollmentException(StudentEnrollmentErrorCode.GRADE_RESTRICTED);
        }
    }

    private void validatePrerequisites(StudentProfile student, CourseOffering offering) {
        List<CoursePrerequisite> prerequisites = coursePrerequisiteRepository
            .findBySubjectIdInAndActiveTrue(List.of(offering.getSubject().getId()));
        if (prerequisites.isEmpty()) {
            return;
        }
        Set<Long> completedSubjectIds = findCompletedSubjectIds(student, prerequisites);
        if (!missingPrerequisiteSubjectIds(prerequisites, completedSubjectIds).isEmpty()) {
            throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.PREREQUISITE_REQUIRED
            );
        }
    }

    private void validateCreditLimit(
        StudentProfile student,
        Semester semester,
        CourseOffering offering
    ) {
        validateCreditLimit(student, semester, offering, null);
    }

    private void validateCreditLimit(
        StudentProfile student,
        Semester semester,
        CourseOffering offering,
        Long excludedEnrollmentId
    ) {
        int currentCredit = currentActiveCredit(student, semester, excludedEnrollmentId);
        int maxCredit = maxCredit(student, semester);
        if (currentCredit + offering.getSubject().getCredit() > maxCredit) {
            throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.CREDIT_LIMIT_EXCEEDED
            );
        }
    }

    private void validateNoTimeConflict(
        StudentProfile student,
        Semester semester,
        CourseOffering offering
    ) {
        validateNoTimeConflict(student, semester, offering, null);
    }

    private void validateNoTimeConflict(
        StudentProfile student,
        Semester semester,
        CourseOffering offering,
        Long excludedEnrollmentId
    ) {
        List<CourseTime> candidateTimes = findCourseTimesByOfferingId(List.of(offering.getId()))
            .getOrDefault(offering.getId(), List.of());
        List<CourseTime> activeTimes = findMyActiveCourseTimes(student, semester, excludedEnrollmentId);
        if (hasTimeConflict(candidateTimes, activeTimes)) {
            throw new StudentEnrollmentException(StudentEnrollmentErrorCode.TIME_CONFLICT);
        }
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

    private Long promoteFirstWaitingIfEligible(CourseOffering offering) {
        List<Enrollment> waitingEnrollments =
            enrollmentRepository.findByCourseOfferingIdAndStatusOrderByAppliedAtAscIdAsc(
                offering.getId(),
                EnrollmentStatus.WAITING
            );
        if (waitingEnrollments.isEmpty()) {
            return null;
        }

        Enrollment firstWaiting = waitingEnrollments.get(0);
        StudentProfile waitingStudent = firstWaiting.getStudent();

        try {
            validateGradeAllowed(waitingStudent, offering);
            validatePrerequisites(waitingStudent, offering);
            validateCreditLimit(waitingStudent, offering.getSemester(), offering, firstWaiting.getId());
            validateNoTimeConflict(waitingStudent, offering.getSemester(), offering, firstWaiting.getId());
        } catch (StudentEnrollmentException ignored) {
            // Current policy: keep an ineligible first waiting enrollment as WAITING and
            // stop auto-promotion. Skipping to later waiters is deferred to a future step.
            return null;
        }

        firstWaiting.promoteToEnrolled(LocalDateTime.now());
        return firstWaiting.getId();
    }

    private String normalizeMode(String mode) {
        String normalizedMode = normalizeCondition(mode);
        if (normalizedMode == null) {
            return CONDITION_MODE;
        }
        String lowerMode = normalizedMode.toLowerCase(Locale.ROOT);
        if (!CONDITION_MODE.equals(lowerMode) && !DIRECT_MODE.equals(lowerMode)) {
            throw new StudentEnrollmentException(StudentEnrollmentErrorCode.INVALID_SEARCH_MODE);
        }
        return lowerMode;
    }

    private String normalizeCondition(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value.trim();
    }

    private Comparator<CourseOffering> courseOfferingComparator() {
        return Comparator
            .comparing(
                (CourseOffering offering) -> offering.getSubject().getCode(),
                Comparator.nullsLast(String::compareTo)
            )
            .thenComparing(CourseOffering::getId, Comparator.nullsLast(Long::compareTo));
    }

    private Comparator<Enrollment> enrollmentComparator() {
        return Comparator
            .comparing(
                (Enrollment enrollment) -> enrollment.getCourseOffering().getSubject().getCode(),
                Comparator.nullsLast(String::compareTo)
            )
            .thenComparing(Enrollment::getId, Comparator.nullsLast(Long::compareTo));
    }

    private Comparator<WishCourse> wishCourseComparator() {
        return Comparator
            .comparing(
                (WishCourse wishCourse) -> wishCourse.getCourseOffering().getSubject().getCode(),
                Comparator.nullsLast(String::compareTo)
            )
            .thenComparing(WishCourse::getId, Comparator.nullsLast(Long::compareTo));
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

    private Map<Long, Set<Integer>> findAllowedGradesByOfferingId(List<Long> offeringIds) {
        if (offeringIds.isEmpty()) {
            return Map.of();
        }
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
        return findMyActiveCourseTimes(student, semester, null);
    }

    private List<CourseTime> findMyActiveCourseTimes(
        StudentProfile student,
        Semester semester,
        Long excludedEnrollmentId
    ) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndSemesterIdAndStatusIn(
            student.getId(),
            semester.getId(),
            ACTIVE_STATUSES
        );
        List<Long> offeringIds = enrollments.stream()
            .filter(enrollment -> excludedEnrollmentId == null
                || !excludedEnrollmentId.equals(enrollment.getId()))
            .map(enrollment -> enrollment.getCourseOffering().getId())
            .toList();
        if (offeringIds.isEmpty()) {
            return List.of();
        }
        return findCourseTimesByOfferingId(offeringIds).values().stream()
            .flatMap(Collection::stream)
            .toList();
    }

    private int waitNo(Enrollment enrollment) {
        List<Enrollment> waitingEnrollments =
            enrollmentRepository.findByCourseOfferingIdAndStatusOrderByAppliedAtAscIdAsc(
                enrollment.getCourseOffering().getId(),
                EnrollmentStatus.WAITING
            );
        for (int i = 0; i < waitingEnrollments.size(); i++) {
            if (waitingEnrollments.get(i).getId().equals(enrollment.getId())) {
                return i + 1;
            }
        }
        return 0;
    }

    private StudentEnrolledCourseResponse toEnrolledCourseResponse(
        Enrollment enrollment,
        List<CourseTime> courseTimes
    ) {
        CourseOffering offering = enrollment.getCourseOffering();
        return new StudentEnrolledCourseResponse(
            enrollment.getId(),
            offering.getId(),
            offering.getSubject().getCode(),
            offering.getSubject().getName(),
            offering.getProfessor().getMember().getName(),
            offering.getSubject().getCredit(),
            toCourseTimeText(courseTimes),
            enrollment.getSource().name()
        );
    }

    private StudentWaitingCourseResponse toWaitingCourseResponse(
        Enrollment enrollment,
        List<CourseTime> courseTimes,
        EnrollmentStats stats,
        int waitNo
    ) {
        CourseOffering offering = enrollment.getCourseOffering();
        return new StudentWaitingCourseResponse(
            enrollment.getId(),
            offering.getId(),
            offering.getSubject().getCode(),
            offering.getSubject().getName(),
            offering.getProfessor().getMember().getName(),
            toCourseTimeText(courseTimes),
            stats.enrolledCount().intValue(),
            offering.getCapacity(),
            waitNo
        );
    }

    private StudentManualRequiredCourseResponse toManualRequiredResponse(
        WishCourse wishCourse,
        List<CourseTime> courseTimes,
        boolean actionAvailable
    ) {
        CourseOffering offering = wishCourse.getCourseOffering();
        return new StudentManualRequiredCourseResponse(
            wishCourse.getId(),
            offering.getId(),
            offering.getSubject().getCode(),
            offering.getSubject().getName(),
            offering.getProfessor().getMember().getName(),
            offering.getSubject().getCredit(),
            toCourseTimeText(courseTimes),
            wishCourse.getResult().name(),
            resultMessage(wishCourse),
            actionAvailable
        );
    }

    private StudentEnrollmentCourseResponse toSearchResponse(
        CourseOffering offering,
        List<CourseTime> courseTimes,
        EnrollmentStats stats,
        Enrollment myEnrollment,
        Set<Integer> allowedGrades,
        List<CoursePrerequisite> prerequisites,
        Set<Long> completedSubjectIds,
        List<CourseTime> myActiveTimes,
        Integer studentGrade,
        int currentCredit,
        int maxCredit,
        boolean enrollmentOpen
    ) {
        CourseStatusDecision decision = decideStatus(
            offering,
            courseTimes,
            stats,
            myEnrollment,
            allowedGrades,
            prerequisites,
            completedSubjectIds,
            myActiveTimes,
            studentGrade,
            currentCredit,
            maxCredit,
            enrollmentOpen
        );
        return new StudentEnrollmentCourseResponse(
            offering.getId(),
            offering.getSubject().getCode(),
            offering.getSubject().getName(),
            offering.getSubject().getDepartment().getName(),
            offering.getProfessor().getMember().getName(),
            offering.getSubject().getCredit(),
            toCourseTimeText(courseTimes),
            allowedGradeText(allowedGrades),
            prerequisiteText(prerequisites),
            stats.enrolledCount(),
            offering.getCapacity(),
            stats.waitingCount(),
            decision.status().name(),
            decision.message()
        );
    }

    private CourseStatusDecision decideStatus(
        CourseOffering offering,
        List<CourseTime> courseTimes,
        EnrollmentStats stats,
        Enrollment myEnrollment,
        Set<Integer> allowedGrades,
        List<CoursePrerequisite> prerequisites,
        Set<Long> completedSubjectIds,
        List<CourseTime> myActiveTimes,
        Integer studentGrade,
        int currentCredit,
        int maxCredit,
        boolean enrollmentOpen
    ) {
        if (myEnrollment != null && EnrollmentStatus.ENROLLED.equals(myEnrollment.getStatus())) {
            return new CourseStatusDecision(StudentEnrollmentCourseStatus.ENROLLED, "Already enrolled.");
        }
        if (myEnrollment != null && EnrollmentStatus.WAITING.equals(myEnrollment.getStatus())) {
            return new CourseStatusDecision(StudentEnrollmentCourseStatus.WAITING, "Already waiting.");
        }
        if (!enrollmentOpen) {
            return new CourseStatusDecision(StudentEnrollmentCourseStatus.PERIOD_CLOSED, "Enrollment period is closed.");
        }
        if (!allowedGrades.isEmpty() && !allowedGrades.contains(studentGrade)) {
            return new CourseStatusDecision(StudentEnrollmentCourseStatus.GRADE_RESTRICTED, "Grade is restricted.");
        }
        Set<Long> missingPrerequisiteSubjectIds = missingPrerequisiteSubjectIds(
            prerequisites,
            completedSubjectIds
        );
        if (!missingPrerequisiteSubjectIds.isEmpty()) {
            return new CourseStatusDecision(StudentEnrollmentCourseStatus.PREREQUISITE_REQUIRED, "Prerequisite is required.");
        }
        if (hasTimeConflict(courseTimes, myActiveTimes)) {
            return new CourseStatusDecision(StudentEnrollmentCourseStatus.TIME_CONFLICT, "Course time conflicts.");
        }
        if (currentCredit + offering.getSubject().getCredit() > maxCredit) {
            return new CourseStatusDecision(StudentEnrollmentCourseStatus.CREDIT_LIMIT, "Credit limit exceeded.");
        }
        if (stats.enrolledCount() >= offering.getCapacity()) {
            return new CourseStatusDecision(StudentEnrollmentCourseStatus.FULL, "Capacity is full.");
        }
        return new CourseStatusDecision(StudentEnrollmentCourseStatus.AVAILABLE, "Available.");
    }

    private Set<Long> missingPrerequisiteSubjectIds(
        List<CoursePrerequisite> prerequisites,
        Set<Long> completedSubjectIds
    ) {
        Set<Long> missingIds = new HashSet<>();
        for (CoursePrerequisite prerequisite : prerequisites) {
            Long subjectId = prerequisite.getPrerequisiteSubject().getId();
            if (!completedSubjectIds.contains(subjectId)) {
                missingIds.add(subjectId);
            }
        }
        return missingIds;
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

    private String allowedGradeText(Set<Integer> allowedGrades) {
        if (allowedGrades.isEmpty()) {
            return "All grades";
        }
        return allowedGrades.stream()
            .map(grade -> grade + " grade")
            .collect(Collectors.joining(", "));
    }

    private String prerequisiteText(List<CoursePrerequisite> prerequisites) {
        if (prerequisites.isEmpty()) {
            return "None";
        }
        return prerequisites.stream()
            .map(prerequisite -> prerequisite.getPrerequisiteSubject().getName())
            .collect(Collectors.joining(", "));
    }

    private String resultMessage(WishCourse wishCourse) {
        if (wishCourse.getResultMessage() != null && !wishCourse.getResultMessage().isBlank()) {
            return wishCourse.getResultMessage();
        }
        return switch (wishCourse.getResult()) {
            case NOT_SELECTED -> "Auto apply was not selected.";
            case PENDING -> "Auto apply result is pending.";
            case DONE -> "Auto apply is done.";
            case OVER_CAPACITY -> "Manual enrollment is required because capacity was exceeded.";
            case NEEDS_MANUAL -> "Manual enrollment is required.";
            case TIME_CONFLICT -> "Manual enrollment is required because of a time conflict.";
        };
    }

    private String toCourseTimeText(List<CourseTime> courseTimes) {
        if (courseTimes.isEmpty()) {
            return "-";
        }
        return courseTimes.stream()
            .sorted(Comparator
                .comparing(CourseTime::getDayOfWeek)
                .thenComparing(CourseTime::getStartPeriod))
            .map(time -> "%s %d~%d".formatted(
                time.getDayOfWeek().name(),
                time.getStartPeriod(),
                time.getEndPeriod()
            ))
            .collect(Collectors.joining(", "));
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

    private record CourseStatusDecision(
        StudentEnrollmentCourseStatus status,
        String message
    ) {
    }
}
