package com.example.CampusFlowServer.domain.student.catalog.service;

import com.example.CampusFlowServer.domain.course.entity.CourseOffering;
import com.example.CampusFlowServer.domain.course.entity.CourseTime;
import com.example.CampusFlowServer.domain.course.enums.DayOfWeek;
import com.example.CampusFlowServer.domain.course.repository.CourseOfferingRepository;
import com.example.CampusFlowServer.domain.course.repository.CourseTimeRepository;
import com.example.CampusFlowServer.domain.courseplan.entity.CoursePlan;
import com.example.CampusFlowServer.domain.courseplan.enums.CoursePlanStatus;
import com.example.CampusFlowServer.domain.courseplan.enums.CoursePlanVersionType;
import com.example.CampusFlowServer.domain.courseplan.repository.CoursePlanContentRepository;
import com.example.CampusFlowServer.domain.courseplan.repository.CoursePlanRepository;
import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import com.example.CampusFlowServer.domain.student.catalog.dto.StudentCatalogCourseResponse;
import com.example.CampusFlowServer.domain.student.catalog.dto.StudentCatalogCourseSearchRequest;
import com.example.CampusFlowServer.domain.student.catalog.exception.StudentCatalogErrorCode;
import com.example.CampusFlowServer.domain.student.catalog.exception.StudentCatalogException;
import com.example.CampusFlowServer.domain.student.catalog.specification.CourseOfferingCatalogSpecification;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCatalogService {

    private static final String CONDITION_MODE = "condition";
    private static final String DIRECT_MODE = "direct";

    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseTimeRepository courseTimeRepository;
    private final CoursePlanRepository coursePlanRepository;
    private final CoursePlanContentRepository coursePlanContentRepository;

    public List<StudentCatalogCourseResponse> searchCourses(
        StudentCatalogCourseSearchRequest request
    ) {
        validateRequired(request);

        String mode = normalizeMode(request.mode());
        Specification<CourseOffering> specification = switch (mode) {
            case CONDITION_MODE -> CourseOfferingCatalogSpecification.byCondition(//조건 조회 검색
                request.year(),
                request.term(),
                normalizeCondition(request.collegeName()),
                normalizeCondition(request.departmentName()),
                normalizeCondition(request.category())
            );
            case DIRECT_MODE -> CourseOfferingCatalogSpecification.byDirect( //직접 입력 검색
                request.year(),
                request.term(),
                normalizeCondition(request.subjectName()),
                normalizeCondition(request.professorName()),
                normalizeCondition(request.departmentName())
            );
            default -> throw new StudentCatalogException(
                StudentCatalogErrorCode.INVALID_SEARCH_MODE
            );
        };

        List<CourseOffering> courseOfferings = courseOfferingRepository.findAll(specification)
            .stream()
            .sorted(courseOfferingComparator())
            .toList(); //조회 결과를 정렬(교과목 코드 오름차순, 개설 강의 ID오름차순)
        if (courseOfferings.isEmpty()) {
            return List.of();
        }

        List<Long> courseOfferingIds = courseOfferings.stream()
            .map(CourseOffering::getId)
            .toList();
        Map<Long, List<CourseTime>> courseTimesByOfferingId =
            findCourseTimesByOfferingId(courseOfferingIds); //개설 강의 ID 목록 기준 강의 시간 일괄 조회
        Map<Long, CoursePlan> coursePlansByOfferingId = findCoursePlansByOfferingId(
            courseOfferingIds
        ); //개설 강의 ID 목록 기준 강의계획서 일괄 조회
        Set<Long> publishedCoursePlanIds = findPublishedCoursePlanIds(
            coursePlansByOfferingId.values()
        ); //PUBLISHED 버전 강의계획서 내용 존재 여부 확인

        return courseOfferings.stream()
            .map(courseOffering -> toResponse(//응답 DTO변환
                courseOffering,
                courseTimesByOfferingId.getOrDefault(courseOffering.getId(), List.of()),
                coursePlansByOfferingId.get(courseOffering.getId()),
                publishedCoursePlanIds
            ))
            .toList();
    }

    private void validateRequired(StudentCatalogCourseSearchRequest request) {
        if (request.year() == null) {//학년도 검증
            throw new StudentCatalogException(StudentCatalogErrorCode.REQUIRED_YEAR);
        }
        if (request.term() == null) { //학기 검증
            throw new StudentCatalogException(StudentCatalogErrorCode.REQUIRED_TERM);
        }
    }

    private String normalizeMode(String mode) { //검색 모드 정규화
        String normalizedMode = normalizeCondition(mode);
        if (normalizedMode == null) {
            return CONDITION_MODE;
        }
        String lowerMode = normalizedMode.toLowerCase(Locale.ROOT);
        if (!CONDITION_MODE.equals(lowerMode) && !DIRECT_MODE.equals(lowerMode)) {
            throw new StudentCatalogException(StudentCatalogErrorCode.INVALID_SEARCH_MODE);
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
            .thenComparing(
                CourseOffering::getId,
                Comparator.nullsLast(Long::compareTo)
            );
    }

    private Map<Long, List<CourseTime>> findCourseTimesByOfferingId(List<Long> courseOfferingIds) {
        return courseTimeRepository
            .findByCourseOfferingIdInOrderByCourseOfferingIdAscDayOfWeekAscStartPeriodAsc(
                courseOfferingIds
            )
            .stream()
            .collect(Collectors.groupingBy(courseTime -> courseTime.getCourseOffering().getId()));
    }

    private Map<Long, CoursePlan> findCoursePlansByOfferingId(List<Long> courseOfferingIds) {
        return coursePlanRepository.findByCourseOfferingIdIn(courseOfferingIds)
            .stream()
            .collect(Collectors.toMap(
                coursePlan -> coursePlan.getCourseOffering().getId(),
                Function.identity()
            ));
    }

    private Set<Long> findPublishedCoursePlanIds(Collection<CoursePlan> coursePlans) {
        List<Long> publishableCoursePlanIds = coursePlans.stream()
            .filter(coursePlan -> CoursePlanStatus.PUBLISHED.equals(coursePlan.getStatus())
                || CoursePlanStatus.PUBLISHED_DRAFT.equals(coursePlan.getStatus()))
            .map(CoursePlan::getId)
            .toList();
        if (publishableCoursePlanIds.isEmpty()) {
            return Set.of();
        }

        return coursePlanContentRepository
            .findByCoursePlanIdInAndVersionType(
                publishableCoursePlanIds,
                CoursePlanVersionType.PUBLISHED
            )
            .stream()
            .map(coursePlanContent -> coursePlanContent.getCoursePlan().getId())
            .collect(Collectors.toSet());
    }

    private StudentCatalogCourseResponse toResponse(
        CourseOffering courseOffering,
        List<CourseTime> courseTimes,
        CoursePlan coursePlan,
        Set<Long> publishedCoursePlanIds
    ) {
        boolean hasPublishedCoursePlan =
            coursePlan != null && publishedCoursePlanIds.contains(coursePlan.getId());

        return new StudentCatalogCourseResponse(
            courseOffering.getId(),
            courseOffering.getSubject().getCode(),
            courseOffering.getSubject().getName(),
            courseOffering.getSubject().getDepartment().getName(),
            courseOffering.getSubject().getDepartment().getCollegeName(),
            courseOffering.getSubject().getCategory().name(),
            courseOffering.getProfessor().getMember().getName(),
            courseOffering.getSubject().getCredit(),
            toCourseTimeText(courseTimes),
            courseOffering.getCapacity(),
            coursePlan == null ? "EMPTY" : coursePlan.getStatus().name(),
            hasPublishedCoursePlan
        );
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
}
