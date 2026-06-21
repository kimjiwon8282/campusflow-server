package com.example.CampusFlowServer.domain.student.enrollment.service;

import com.example.CampusFlowServer.domain.course.entity.CourseOffering;
import com.example.CampusFlowServer.domain.course.repository.CourseOfferingRepository;
import com.example.CampusFlowServer.domain.enrollment.entity.CourseAllowedGrade;
import com.example.CampusFlowServer.domain.enrollment.entity.CoursePrerequisite;
import com.example.CampusFlowServer.domain.enrollment.repository.CompletedCourseRepository;
import com.example.CampusFlowServer.domain.enrollment.repository.CourseAllowedGradeRepository;
import com.example.CampusFlowServer.domain.enrollment.repository.CoursePrerequisiteRepository;
import com.example.CampusFlowServer.domain.member.entity.StudentProfile;
import com.example.CampusFlowServer.domain.member.repository.StudentProfileRepository;
import com.example.CampusFlowServer.domain.semester.entity.Semester;
import com.example.CampusFlowServer.domain.semester.entity.SemesterSchedule;
import com.example.CampusFlowServer.domain.semester.enums.SemesterScheduleType;
import com.example.CampusFlowServer.domain.semester.repository.SemesterScheduleRepository;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentErrorCode;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentEnrollmentPreValidationService {

    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseAllowedGradeRepository courseAllowedGradeRepository;
    private final CoursePrerequisiteRepository coursePrerequisiteRepository;
    private final CompletedCourseRepository completedCourseRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SemesterScheduleRepository semesterScheduleRepository;

    public EnrollmentPreValidationResult validate(Long memberId, Long courseOfferingId) {
        StudentProfile student = studentProfileRepository.findByMemberId(memberId)
            .orElseThrow(() -> new StudentEnrollmentException(
                StudentEnrollmentErrorCode.STUDENT_PROFILE_NOT_FOUND
            ));
        CourseOffering offering = courseOfferingRepository.findById(courseOfferingId)
            .orElseThrow(() -> new StudentEnrollmentException(
                StudentEnrollmentErrorCode.COURSE_OFFERING_NOT_FOUND
            ));

        validateEnrollmentPeriod(offering.getSemester());
        validateGradeAllowed(student, offering);
        validatePrerequisites(student, offering);

        return new EnrollmentPreValidationResult(student.getId(), offering.getId());
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

    private java.util.Map<Long, Set<Integer>> findAllowedGradesByOfferingId(List<Long> offeringIds) {
        if (offeringIds.isEmpty()) {
            return java.util.Map.of();
        }
        java.util.Map<Long, Set<Integer>> result = new java.util.HashMap<>();
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
}
