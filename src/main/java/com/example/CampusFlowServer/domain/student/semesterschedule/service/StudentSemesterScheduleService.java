package com.example.CampusFlowServer.domain.student.semesterschedule.service;

import com.example.CampusFlowServer.domain.semester.entity.Semester;
import com.example.CampusFlowServer.domain.semester.entity.SemesterSchedule;
import com.example.CampusFlowServer.domain.semester.enums.SemesterScheduleType;
import com.example.CampusFlowServer.domain.semester.repository.SemesterRepository;
import com.example.CampusFlowServer.domain.semester.repository.SemesterScheduleRepository;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentErrorCode;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentException;
import com.example.CampusFlowServer.domain.student.semesterschedule.dto.StudentSemesterScheduleStatusRequest;
import com.example.CampusFlowServer.domain.student.semesterschedule.dto.StudentSemesterScheduleStatusResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentSemesterScheduleService {

    private static final String ENROLLMENT_OPEN_MESSAGE = "수강신청 기간입니다.";
    private static final String ENROLLMENT_CLOSED_MESSAGE = "수강신청 기간이 아닙니다.";
    private static final String WISH_OPEN_MESSAGE = "희망과목 담기 기간입니다.";
    private static final String WISH_CLOSED_MESSAGE = "희망과목 담기 기간이 아닙니다.";

    private final SemesterRepository semesterRepository;
    private final SemesterScheduleRepository semesterScheduleRepository;

    public StudentSemesterScheduleStatusResponse getStatus(
        StudentSemesterScheduleStatusRequest request
    ) {
        validateRequired(request.year(), request.term());
        Semester semester = semesterRepository.findByYearAndTerm(request.year(), request.term())
            .orElseThrow(() -> new StudentEnrollmentException(
                StudentEnrollmentErrorCode.SEMESTER_NOT_FOUND
            ));
        LocalDateTime now = LocalDateTime.now();
        boolean enrollmentOpen = isOpen(semester, SemesterScheduleType.ENROLLMENT, now);
        boolean wishOpen = isOpen(semester, SemesterScheduleType.WISHLIST, now);

        return new StudentSemesterScheduleStatusResponse(
            enrollmentOpen,
            wishOpen,
            enrollmentOpen ? ENROLLMENT_OPEN_MESSAGE : ENROLLMENT_CLOSED_MESSAGE,
            wishOpen ? WISH_OPEN_MESSAGE : WISH_CLOSED_MESSAGE
        );
    }

    private boolean isOpen(
        Semester semester,
        SemesterScheduleType type,
        LocalDateTime now
    ) {
        return semesterScheduleRepository.findBySemesterIdAndType(semester.getId(), type)
            .map(schedule -> isOpen(schedule, now))
            .orElse(false);
    }

    private boolean isOpen(SemesterSchedule schedule, LocalDateTime now) {
        if (schedule.isAlwaysOpen()) {
            return true;
        }
        return schedule.getStartAt() != null
            && schedule.getEndAt() != null
            && !now.isBefore(schedule.getStartAt())
            && !now.isAfter(schedule.getEndAt());
    }

    private void validateRequired(
        Integer year,
        com.example.CampusFlowServer.domain.semester.enums.SemesterTerm term
    ) {
        if (year == null) {
            throw new StudentEnrollmentException(StudentEnrollmentErrorCode.REQUIRED_YEAR);
        }
        if (term == null) {
            throw new StudentEnrollmentException(StudentEnrollmentErrorCode.REQUIRED_TERM);
        }
    }
}
