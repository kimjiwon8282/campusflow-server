package com.example.CampusFlowServer.domain.student.wishcourse.service;

import com.example.CampusFlowServer.domain.course.entity.CourseOffering;
import com.example.CampusFlowServer.domain.course.entity.CourseTime;
import com.example.CampusFlowServer.domain.course.enums.DayOfWeek;
import com.example.CampusFlowServer.domain.course.repository.CourseOfferingRepository;
import com.example.CampusFlowServer.domain.course.repository.CourseTimeRepository;
import com.example.CampusFlowServer.domain.enrollment.entity.CourseAllowedGrade;
import com.example.CampusFlowServer.domain.enrollment.entity.CoursePrerequisite;
import com.example.CampusFlowServer.domain.enrollment.entity.WishCourse;
import com.example.CampusFlowServer.domain.enrollment.enums.WishAutoApplyResult;
import com.example.CampusFlowServer.domain.enrollment.repository.CompletedCourseRepository;
import com.example.CampusFlowServer.domain.enrollment.repository.CourseAllowedGradeRepository;
import com.example.CampusFlowServer.domain.enrollment.repository.CoursePrerequisiteRepository;
import com.example.CampusFlowServer.domain.enrollment.repository.WishCourseCountProjection;
import com.example.CampusFlowServer.domain.enrollment.repository.WishCourseRepository;
import com.example.CampusFlowServer.domain.member.entity.StudentProfile;
import com.example.CampusFlowServer.domain.member.repository.StudentProfileRepository;
import com.example.CampusFlowServer.domain.semester.entity.Semester;
import com.example.CampusFlowServer.domain.semester.entity.SemesterSchedule;
import com.example.CampusFlowServer.domain.semester.enums.SemesterScheduleType;
import com.example.CampusFlowServer.domain.semester.repository.SemesterScheduleRepository;
import com.example.CampusFlowServer.domain.semester.repository.SemesterRepository;
import com.example.CampusFlowServer.domain.student.catalog.specification.CourseOfferingCatalogSpecification;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseAutoApplyCheckRequest;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseAutoApplyRequest;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseCandidateResponse;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseCandidateSearchRequest;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseCreateRequest;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseItemResponse;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseListResponse;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseSemesterRequest;
import com.example.CampusFlowServer.domain.student.wishcourse.dto.StudentWishCourseSummaryResponse;
import com.example.CampusFlowServer.domain.student.wishcourse.exception.StudentWishCourseErrorCode;
import com.example.CampusFlowServer.domain.student.wishcourse.exception.StudentWishCourseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
public class StudentWishCourseService {

    private static final String CONDITION_MODE = "condition";
    private static final String DIRECT_MODE = "direct";

    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseTimeRepository courseTimeRepository;
    private final WishCourseRepository wishCourseRepository;
    private final CourseAllowedGradeRepository courseAllowedGradeRepository;
    private final CoursePrerequisiteRepository coursePrerequisiteRepository;
    private final CompletedCourseRepository completedCourseRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SemesterRepository semesterRepository;
    private final SemesterScheduleRepository semesterScheduleRepository;

    public List<StudentWishCourseCandidateResponse> searchCandidates(
        Long memberId,
        StudentWishCourseCandidateSearchRequest request
    ) {
        validateRequired(request.year(), request.term());
        StudentProfile student = findStudent(memberId);
        findSemester(request.year(), request.term());

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
            default -> throw new StudentWishCourseException(
                StudentWishCourseErrorCode.INVALID_SEARCH_MODE
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
        Map<Long, List<CourseTime>> courseTimesByOfferingId = findCourseTimesByOfferingId(offeringIds);
        Map<Long, WishStats> wishStatsByOfferingId = findWishStatsByOfferingId(offeringIds);
        Map<Long, WishCourse> myWishesByOfferingId = wishCourseRepository
            .findByStudentIdAndCourseOfferingIdIn(student.getId(), offeringIds)
            .stream()
            .collect(Collectors.toMap(
                wishCourse -> wishCourse.getCourseOffering().getId(),
                Function.identity()
            ));

        return offerings.stream()
            .map(offering -> toCandidateResponse(
                offering,
                courseTimesByOfferingId.getOrDefault(offering.getId(), List.of()),
                wishStatsByOfferingId.getOrDefault(offering.getId(), WishStats.EMPTY),
                myWishesByOfferingId.get(offering.getId())
            ))
            .toList();
    }

    public StudentWishCourseListResponse getMyWishCourses(
        Long memberId,
        StudentWishCourseSemesterRequest request
    ) {
        validateRequired(request.year(), request.term());
        StudentProfile student = findStudent(memberId);
        Semester semester = findSemester(request.year(), request.term());

        List<WishCourse> wishCourses = wishCourseRepository
            .findByStudentIdAndSemesterId(student.getId(), semester.getId())
            .stream()
            .sorted(Comparator
                .comparing((WishCourse wishCourse) ->
                    wishCourse.getCourseOffering().getSubject().getCode(),
                    Comparator.nullsLast(String::compareTo)
                )
                .thenComparing(WishCourse::getId, Comparator.nullsLast(Long::compareTo)))
            .toList();
        if (wishCourses.isEmpty()) {
            return new StudentWishCourseListResponse(
                new StudentWishCourseSummaryResponse(0, 0, 0, 0),
                List.of()
            );
        }

        return buildWishCourseListResponse(wishCourses);
    }

    @Transactional
    public StudentWishCourseItemResponse createWishCourse(
        Long memberId,
        StudentWishCourseCreateRequest request
    ) {
        StudentProfile student = findStudent(memberId);
        CourseOffering offering = findCourseOffering(request.courseOfferingId());
        Semester semester = offering.getSemester();
        validateWishlistPeriod(semester);

        if (wishCourseRepository.existsByStudentIdAndSemesterIdAndCourseOfferingId(
            student.getId(),
            semester.getId(),
            offering.getId()
        )) {
            throw new StudentWishCourseException(
                StudentWishCourseErrorCode.DUPLICATE_WISH_COURSE
            );
        }

        WishCourse wishCourse = wishCourseRepository.save(WishCourse.create(
            student,
            semester,
            offering,
            false,
            WishAutoApplyResult.NOT_SELECTED,
            null
        ));

        return toSingleItemResponse(wishCourse);
    }

    @Transactional
    public void deleteWishCourse(Long memberId, Long wishCourseId) {
        StudentProfile student = findStudent(memberId);
        WishCourse wishCourse = findWishCourse(wishCourseId);
        validateOwner(wishCourse, student);
        validateWishlistPeriod(wishCourse.getSemester());

        wishCourseRepository.delete(wishCourse);
    }

    @Transactional
    public StudentWishCourseItemResponse updateAutoApply(
        Long memberId,
        Long wishCourseId,
        StudentWishCourseAutoApplyRequest request
    ) {
        StudentProfile student = findStudent(memberId);
        WishCourse wishCourse = findWishCourse(wishCourseId);
        validateOwner(wishCourse, student);
        validateWishlistPeriod(wishCourse.getSemester());

        if (request.autoApply() == null) {
            throw new StudentWishCourseException(StudentWishCourseErrorCode.REQUIRED_AUTO_APPLY);
        }
        if (request.autoApply()) {
            wishCourse.enableAutoApply();
        } else {
            wishCourse.disableAutoApply();
        }

        return toSingleItemResponse(wishCourse);
    }

    @Transactional
    public StudentWishCourseListResponse checkAutoApplyResults(
        Long memberId,
        StudentWishCourseAutoApplyCheckRequest request
    ) {
        validateRequired(request.year(), request.term());
        StudentProfile student = findStudent(memberId);
        Semester semester = findSemester(request.year(), request.term());
        validateWishlistPeriod(semester);

        List<WishCourse> wishCourses = wishCourseRepository.findByStudentIdAndSemesterId(
            student.getId(),
            semester.getId()
        );
        if (wishCourses.isEmpty()) {
            return new StudentWishCourseListResponse(
                new StudentWishCourseSummaryResponse(0, 0, 0, 0),
                List.of()
            );
        }

        List<Long> offeringIds = wishCourses.stream()
            .map(wishCourse -> wishCourse.getCourseOffering().getId())
            .toList();
        Map<Long, List<CourseTime>> courseTimesByOfferingId = findCourseTimesByOfferingId(offeringIds);
        Map<Long, Set<Integer>> allowedGradesByOfferingId =
            findAllowedGradesByOfferingId(wishCourses);
        Map<Long, List<String>> missingPrerequisiteNamesBySubjectId =
            findMissingPrerequisiteNamesBySubjectId(student, wishCourses);
        Set<Long> conflictWishCourseIds = findConflictWishCourseIds(
            wishCourses,
            courseTimesByOfferingId
        );
        Set<Long> overCapacityWishCourseIds = findOverCapacityWishCourseIds(wishCourses);

        for (WishCourse wishCourse : wishCourses) {
            Long offeringId = wishCourse.getCourseOffering().getId();
            if (!wishCourse.isAutoApply()) {
                wishCourse.markNotSelected("자동신청을 선택하지 않았습니다.");
            } else if (isGradeNotAllowed(
                student,
                allowedGradesByOfferingId.getOrDefault(offeringId, Set.of())
            )) {
                wishCourse.markNeedsManual(
                    gradeRestrictionMessage(allowedGradesByOfferingId.get(offeringId))
                );
            } else if (missingPrerequisiteNamesBySubjectId.containsKey(
                wishCourse.getCourseOffering().getSubject().getId()
            )) {
                wishCourse.markNeedsManual(
                    "선수과목 이수 필요: " + String.join(
                        ", ",
                        missingPrerequisiteNamesBySubjectId.get(
                            wishCourse.getCourseOffering().getSubject().getId()
                        )
                    )
                );
            } else if (conflictWishCourseIds.contains(wishCourse.getId())) {
                wishCourse.markTimeConflict("시간표 중복으로 본 신청이 필요합니다.");
            } else if (overCapacityWishCourseIds.contains(wishCourse.getId())) {
                wishCourse.markOverCapacity("정원 초과로 본 신청이 필요합니다.");
            } else {
                wishCourse.markDone("자동신청 가능 상태입니다.");
            }
        }

        return buildWishCourseListResponse(wishCourses);
    }

    private void validateRequired(
        Integer year,
        com.example.CampusFlowServer.domain.semester.enums.SemesterTerm term
    ) {
        if (year == null) {
            throw new StudentWishCourseException(StudentWishCourseErrorCode.REQUIRED_YEAR);
        }
        if (term == null) {
            throw new StudentWishCourseException(StudentWishCourseErrorCode.REQUIRED_TERM);
        }
    }

    private StudentProfile findStudent(Long memberId) {
        return studentProfileRepository.findByMemberId(memberId)
            .orElseThrow(() -> new StudentWishCourseException(
                StudentWishCourseErrorCode.STUDENT_PROFILE_NOT_FOUND
            ));
    }

    private Semester findSemester(
        Integer year,
        com.example.CampusFlowServer.domain.semester.enums.SemesterTerm term
    ) {
        return semesterRepository.findByYearAndTerm(year, term)
            .orElseThrow(() -> new StudentWishCourseException(
                StudentWishCourseErrorCode.SEMESTER_NOT_FOUND
            ));
    }

    private CourseOffering findCourseOffering(Long courseOfferingId) {
        if (courseOfferingId == null) {
            throw new StudentWishCourseException(
                StudentWishCourseErrorCode.COURSE_OFFERING_NOT_FOUND
            );
        }
        return courseOfferingRepository.findById(courseOfferingId)
            .orElseThrow(() -> new StudentWishCourseException(
                StudentWishCourseErrorCode.COURSE_OFFERING_NOT_FOUND
            ));
    }

    private WishCourse findWishCourse(Long wishCourseId) {
        return wishCourseRepository.findByIdWithDetails(wishCourseId)
            .orElseThrow(() -> new StudentWishCourseException(
                StudentWishCourseErrorCode.WISH_COURSE_NOT_FOUND
            ));
    }

    private void validateOwner(WishCourse wishCourse, StudentProfile student) {
        if (!wishCourse.isOwnedBy(student)) {
            throw new StudentWishCourseException(
                StudentWishCourseErrorCode.WISH_COURSE_ACCESS_DENIED
            );
        }
    }

    private void validateWishlistPeriod(Semester semester) {
        SemesterSchedule schedule = semesterScheduleRepository
            .findBySemesterIdAndType(semester.getId(), SemesterScheduleType.WISHLIST)
            .orElseThrow(() -> new StudentWishCourseException(
                StudentWishCourseErrorCode.WISHLIST_PERIOD_CLOSED
            ));
        if (schedule.isAlwaysOpen()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (schedule.getStartAt() == null
            || schedule.getEndAt() == null
            || now.isBefore(schedule.getStartAt())
            || now.isAfter(schedule.getEndAt())) {
            throw new StudentWishCourseException(
                StudentWishCourseErrorCode.WISHLIST_PERIOD_CLOSED
            );
        }
    }

    private String normalizeMode(String mode) {
        String normalizedMode = normalizeCondition(mode);
        if (normalizedMode == null) {
            return CONDITION_MODE;
        }
        String lowerMode = normalizedMode.toLowerCase(Locale.ROOT);
        if (!CONDITION_MODE.equals(lowerMode) && !DIRECT_MODE.equals(lowerMode)) {
            throw new StudentWishCourseException(StudentWishCourseErrorCode.INVALID_SEARCH_MODE);
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
                (CourseOffering courseOffering) -> courseOffering.getSubject().getCode(),
                Comparator.nullsLast(String::compareTo)
            )
            .thenComparing(CourseOffering::getId, Comparator.nullsLast(Long::compareTo));
    }

    private Map<Long, List<CourseTime>> findCourseTimesByOfferingId(List<Long> offeringIds) {
        return courseTimeRepository
            .findByCourseOfferingIdInOrderByCourseOfferingIdAscDayOfWeekAscStartPeriodAsc(
                offeringIds
            )
            .stream()
            .collect(Collectors.groupingBy(courseTime -> courseTime.getCourseOffering().getId()));
    }

    private Map<Long, WishStats> findWishStatsByOfferingId(List<Long> offeringIds) {
        return wishCourseRepository.countByCourseOfferingIds(offeringIds)
            .stream()
            .collect(Collectors.toMap(
                WishCourseCountProjection::getCourseOfferingId,
                projection -> new WishStats(
                    projection.getWishCount(),
                    projection.getAutoApplyCount()
                )
            ));
    }

    private StudentWishCourseListResponse buildWishCourseListResponse(List<WishCourse> wishCourses) {
        List<WishCourse> sortedWishCourses = wishCourses.stream()
            .sorted(Comparator
                .comparing((WishCourse wishCourse) ->
                    wishCourse.getCourseOffering().getSubject().getCode(),
                    Comparator.nullsLast(String::compareTo)
                )
                .thenComparing(WishCourse::getId, Comparator.nullsLast(Long::compareTo)))
            .toList();
        List<Long> offeringIds = sortedWishCourses.stream()
            .map(wishCourse -> wishCourse.getCourseOffering().getId())
            .toList();
        Map<Long, List<CourseTime>> courseTimesByOfferingId = findCourseTimesByOfferingId(offeringIds);
        Map<Long, WishStats> wishStatsByOfferingId = findWishStatsByOfferingId(offeringIds);

        List<StudentWishCourseItemResponse> items = sortedWishCourses.stream()
            .map(wishCourse -> toItemResponse(
                wishCourse,
                courseTimesByOfferingId.getOrDefault(
                    wishCourse.getCourseOffering().getId(),
                    List.of()
                ),
                wishStatsByOfferingId.getOrDefault(
                    wishCourse.getCourseOffering().getId(),
                    WishStats.EMPTY
                )
            ))
            .toList();

        return new StudentWishCourseListResponse(toSummary(sortedWishCourses), items);
    }

    private Set<Long> findConflictWishCourseIds(
        List<WishCourse> wishCourses,
        Map<Long, List<CourseTime>> courseTimesByOfferingId
    ) {
        List<WishCourse> autoApplyWishes = wishCourses.stream()
            .filter(WishCourse::isAutoApply)
            .toList();
        Set<Long> conflictIds = new HashSet<>();

        for (int i = 0; i < autoApplyWishes.size(); i++) {
            WishCourse first = autoApplyWishes.get(i);
            List<CourseTime> firstTimes = courseTimesByOfferingId.getOrDefault(
                first.getCourseOffering().getId(),
                List.of()
            );
            for (int j = i + 1; j < autoApplyWishes.size(); j++) {
                WishCourse second = autoApplyWishes.get(j);
                List<CourseTime> secondTimes = courseTimesByOfferingId.getOrDefault(
                    second.getCourseOffering().getId(),
                    List.of()
                );
                if (hasTimeConflict(firstTimes, secondTimes)) {
                    conflictIds.add(first.getId());
                    conflictIds.add(second.getId());
                }
            }
        }

        return conflictIds;
    }

    private boolean hasTimeConflict(List<CourseTime> firstTimes, List<CourseTime> secondTimes) {
        for (CourseTime first : firstTimes) {
            for (CourseTime second : secondTimes) {
                if (first.getDayOfWeek().equals(second.getDayOfWeek())
                    && first.getStartPeriod() <= second.getEndPeriod()
                    && second.getStartPeriod() <= first.getEndPeriod()) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<Long, List<String>> findMissingPrerequisiteNamesBySubjectId(
        StudentProfile student,
        List<WishCourse> wishCourses
    ) {
        List<Long> subjectIds = wishCourses.stream()
            .filter(WishCourse::isAutoApply)
            .map(wishCourse -> wishCourse.getCourseOffering().getSubject().getId())
            .distinct()
            .toList();
        if (subjectIds.isEmpty()) {
            return Map.of();
        }

        List<CoursePrerequisite> prerequisites = coursePrerequisiteRepository
            .findBySubjectIdInAndActiveTrue(subjectIds);
        if (prerequisites.isEmpty()) {
            return Map.of();
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

        return prerequisites.stream()
            .filter(prerequisite -> !completedSubjectIds.contains(
                prerequisite.getPrerequisiteSubject().getId()
            ))
            .collect(Collectors.groupingBy(
                prerequisite -> prerequisite.getSubject().getId(),
                Collectors.mapping(
                    prerequisite -> prerequisite.getPrerequisiteSubject().getName(),
                    Collectors.toList()
                )
            ));
    }

    private Map<Long, Set<Integer>> findAllowedGradesByOfferingId(List<WishCourse> wishCourses) {
        List<Long> offeringIds = wishCourses.stream()
            .filter(WishCourse::isAutoApply)
            .map(wishCourse -> wishCourse.getCourseOffering().getId())
            .distinct()
            .toList();
        if (offeringIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Set<Integer>> allowedGradesByOfferingId = new HashMap<>();
        for (CourseAllowedGrade allowedGrade :
            courseAllowedGradeRepository.findByCourseOfferingIdIn(offeringIds)) {
            allowedGradesByOfferingId
                .computeIfAbsent(
                    allowedGrade.getCourseOffering().getId(),
                    ignored -> new TreeSet<>()
                )
                .add(allowedGrade.getGradeLevel());
        }
        return allowedGradesByOfferingId;
    }

    private boolean isGradeNotAllowed(StudentProfile student, Set<Integer> allowedGrades) {
        return !allowedGrades.isEmpty() && !allowedGrades.contains(student.getGrade());
    }

    private String gradeRestrictionMessage(Set<Integer> allowedGrades) {
        String allowedGradeText = allowedGrades.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(", "));
        return "신청 가능 학년이 아닙니다. 허용 학년: " + allowedGradeText + "학년";
    }

    private Set<Long> findOverCapacityWishCourseIds(List<WishCourse> myWishCourses) {
        List<Long> offeringIds = myWishCourses.stream()
            .filter(WishCourse::isAutoApply)
            .map(wishCourse -> wishCourse.getCourseOffering().getId())
            .distinct()
            .toList();
        if (offeringIds.isEmpty()) {
            return Set.of();
        }

        Set<Long> myWishCourseIds = myWishCourses.stream()
            .map(WishCourse::getId)
            .collect(Collectors.toSet());
        Set<Long> overCapacityIds = new HashSet<>();
        Map<Long, List<WishCourse>> autoApplyWishesByOfferingId = wishCourseRepository
            .findByCourseOfferingIdInAndAutoApplyTrue(offeringIds)
            .stream()
            .collect(Collectors.groupingBy(wishCourse -> wishCourse.getCourseOffering().getId()));

        for (List<WishCourse> wishes : autoApplyWishesByOfferingId.values()) {
            List<WishCourse> sortedWishes = new ArrayList<>(wishes);
            sortedWishes.sort(Comparator
                .comparing(WishCourse::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(WishCourse::getId, Comparator.nullsLast(Long::compareTo)));

            int capacity = sortedWishes.get(0).getCourseOffering().getCapacity();
            for (int i = capacity; i < sortedWishes.size(); i++) {
                WishCourse wishCourse = sortedWishes.get(i);
                if (myWishCourseIds.contains(wishCourse.getId())) {
                    overCapacityIds.add(wishCourse.getId());
                }
            }
        }

        return overCapacityIds;
    }

    private StudentWishCourseCandidateResponse toCandidateResponse(
        CourseOffering offering,
        List<CourseTime> courseTimes,
        WishStats stats,
        WishCourse myWish
    ) {
        return new StudentWishCourseCandidateResponse(
            offering.getId(),
            offering.getSubject().getCode(),
            offering.getSubject().getName(),
            offering.getSubject().getDepartment().getName(),
            offering.getSubject().getDepartment().getCollegeName(),
            offering.getSubject().getCategory().name(),
            offering.getProfessor().getMember().getName(),
            offering.getSubject().getCredit(),
            toCourseTimeText(courseTimes),
            offering.getCapacity(),
            stats.wishCount(),
            stats.autoApplyCount(),
            myWish != null,
            myWish != null && myWish.isAutoApply(),
            myWish == null ? null : myWish.getResult(),
            myWish == null ? null : resultMessage(myWish)
        );
    }

    private StudentWishCourseItemResponse toItemResponse(
        WishCourse wishCourse,
        List<CourseTime> courseTimes,
        WishStats stats
    ) {
        CourseOffering offering = wishCourse.getCourseOffering();
        return new StudentWishCourseItemResponse(
            wishCourse.getId(),
            offering.getId(),
            offering.getSubject().getCode(),
            offering.getSubject().getName(),
            offering.getSubject().getDepartment().getName(),
            offering.getProfessor().getMember().getName(),
            offering.getSubject().getCredit(),
            toCourseTimeText(courseTimes),
            offering.getCapacity(),
            stats.wishCount(),
            stats.autoApplyCount(),
            wishCourse.isAutoApply(),
            wishCourse.getResult(),
            resultMessage(wishCourse)
        );
    }

    private StudentWishCourseItemResponse toSingleItemResponse(WishCourse wishCourse) {
        Long offeringId = wishCourse.getCourseOffering().getId();
        Map<Long, List<CourseTime>> courseTimesByOfferingId = findCourseTimesByOfferingId(
            List.of(offeringId)
        );
        Map<Long, WishStats> wishStatsByOfferingId = findWishStatsByOfferingId(List.of(offeringId));

        return toItemResponse(
            wishCourse,
            courseTimesByOfferingId.getOrDefault(offeringId, List.of()),
            wishStatsByOfferingId.getOrDefault(offeringId, WishStats.EMPTY)
        );
    }

    private StudentWishCourseSummaryResponse toSummary(List<WishCourse> wishCourses) {
        long autoApplySelectedCount = wishCourses.stream()
            .filter(WishCourse::isAutoApply)
            .count();
        long autoDoneCount = wishCourses.stream()
            .filter(wishCourse -> WishAutoApplyResult.DONE.equals(wishCourse.getResult()))
            .count();
        long manualRequiredCount = wishCourses.stream()
            .filter(this::requiresManualApply)
            .count();

        return new StudentWishCourseSummaryResponse(
            wishCourses.size(),
            autoApplySelectedCount,
            autoDoneCount,
            manualRequiredCount
        );
    }

    private boolean requiresManualApply(WishCourse wishCourse) {
        WishAutoApplyResult result = wishCourse.getResult();
        return !wishCourse.isAutoApply()
            || WishAutoApplyResult.NOT_SELECTED.equals(result)
            || WishAutoApplyResult.OVER_CAPACITY.equals(result)
            || WishAutoApplyResult.NEEDS_MANUAL.equals(result)
            || WishAutoApplyResult.TIME_CONFLICT.equals(result);
    }

    private String resultMessage(WishCourse wishCourse) {
        if (wishCourse.getResultMessage() != null && !wishCourse.getResultMessage().isBlank()) {
            return wishCourse.getResultMessage();
        }
        return switch (wishCourse.getResult()) {
            case NOT_SELECTED -> "자동신청을 선택하지 않았습니다.";
            case PENDING -> "자동신청 대기 중입니다.";
            case DONE -> "자동신청 가능 상태입니다.";
            case OVER_CAPACITY -> "정원 초과로 본 신청이 필요합니다.";
            case NEEDS_MANUAL -> "본 신청이 필요합니다.";
            case TIME_CONFLICT -> "시간표 중복으로 본 신청이 필요합니다.";
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
            .map(courseTime -> "%s %d~%d교시".formatted(
                toKoreanDay(courseTime.getDayOfWeek()),
                courseTime.getStartPeriod(),
                courseTime.getEndPeriod()
            ))
            .collect(Collectors.joining(", "));
    }

    private String toKoreanDay(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MON -> "월";
            case TUE -> "화";
            case WED -> "수";
            case THU -> "목";
            case FRI -> "금";
            case SAT -> "토";
            case SUN -> "일";
        };
    }

    private record WishStats(Long wishCount, Long autoApplyCount) {

        private static final WishStats EMPTY = new WishStats(0L, 0L);
    }
}
