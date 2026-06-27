package com.example.CampusFlowServer.domain.student.enrollment.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.example.CampusFlowServer.domain.semester.entity.Semester;
import com.example.CampusFlowServer.domain.semester.entity.SemesterSchedule;
import com.example.CampusFlowServer.domain.semester.enums.SemesterScheduleType;
import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import com.example.CampusFlowServer.domain.semester.repository.SemesterScheduleRepository;
import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentBatchLaunchResponse;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentErrorCode;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentException;
import com.example.CampusFlowServer.domain.student.enrollment.service.AutoEnrollmentBatchLaunchService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AutoEnrollmentPreApplySchedulerTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 27);
    private static final LocalDateTime TOMORROW_START = LocalDateTime.of(2026, 6, 28, 0, 0);
    private static final LocalDateTime DAY_AFTER_TOMORROW_START =
        LocalDateTime.of(2026, 6, 29, 0, 0);

    @Mock
    private SemesterScheduleRepository semesterScheduleRepository;

    @Mock
    private AutoEnrollmentBatchLaunchService autoEnrollmentBatchLaunchService;

    @Mock
    private Semester semester;

    private AutoEnrollmentPreApplyScheduler scheduler;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-26T15:00:00Z"), SEOUL);
        scheduler = new AutoEnrollmentPreApplyScheduler(
            semesterScheduleRepository,
            autoEnrollmentBatchLaunchService,
            clock
        );
    }

    @Test
    void launchesBatchWhenEnrollmentStartsTomorrowInSeoul() {
        SemesterSchedule schedule = schedule(
            LocalDateTime.of(2026, 6, 28, 9, 0),
            false
        );
        when(semesterScheduleRepository.findPreApplyTargets(
            SemesterScheduleType.ENROLLMENT,
            TOMORROW_START,
            DAY_AFTER_TOMORROW_START
        )).thenReturn(List.of(schedule));
        when(autoEnrollmentBatchLaunchService.launch(semester, schedule))
            .thenReturn(completedResponse());

        scheduler.runPreApply();

        verify(autoEnrollmentBatchLaunchService).launch(semester, schedule);
    }

    @Test
    void doesNotLaunchWhenScheduleIsNotD1() {
        SemesterSchedule schedule = schedule(
            LocalDateTime.of(2026, 6, 29, 9, 0),
            false
        );
        stubTargets(schedule);

        scheduler.runPreApply();

        verify(autoEnrollmentBatchLaunchService, never()).launch(semester, schedule);
    }

    @Test
    void excludesAlwaysOpenSchedule() {
        SemesterSchedule schedule = schedule(
            LocalDateTime.of(2026, 6, 28, 9, 0),
            true
        );
        stubTargets(schedule);

        scheduler.runPreApply();

        verify(autoEnrollmentBatchLaunchService, never()).launch(semester, schedule);
    }

    @Test
    void excludesScheduleWithoutStartAt() {
        SemesterSchedule schedule = schedule(null, false);
        stubTargets(schedule);

        scheduler.runPreApply();

        verify(autoEnrollmentBatchLaunchService, never()).launch(semester, schedule);
    }

    @Test
    void doesNothingWhenNoEnrollmentScheduleIsFound() {
        stubTargets();

        scheduler.runPreApply();

        verify(autoEnrollmentBatchLaunchService, never()).launch(
            org.mockito.ArgumentMatchers.any(Semester.class),
            org.mockito.ArgumentMatchers.any(SemesterSchedule.class)
        );
    }

    @Test
    void launchesEveryD1SemesterSequentially() {
        Semester secondSemester = org.mockito.Mockito.mock(Semester.class);
        SemesterSchedule first = schedule(
            LocalDateTime.of(2026, 6, 28, 9, 0),
            false
        );
        SemesterSchedule second = org.mockito.Mockito.mock(SemesterSchedule.class);
        when(second.getType()).thenReturn(SemesterScheduleType.ENROLLMENT);
        when(second.isAlwaysOpen()).thenReturn(false);
        when(second.getStartAt()).thenReturn(LocalDateTime.of(2026, 6, 28, 13, 0));
        when(second.getSemester()).thenReturn(secondSemester);
        stubTargets(first, second);
        when(autoEnrollmentBatchLaunchService.launch(semester, first))
            .thenReturn(completedResponse());
        when(autoEnrollmentBatchLaunchService.launch(secondSemester, second))
            .thenReturn(completedResponse());

        scheduler.runPreApply();

        verify(autoEnrollmentBatchLaunchService).launch(semester, first);
        verify(autoEnrollmentBatchLaunchService).launch(secondSemester, second);
    }

    @Test
    void skipsAlreadyCompletedJobWithoutFailure() {
        SemesterSchedule schedule = schedule(
            LocalDateTime.of(2026, 6, 28, 9, 0),
            false
        );
        stubTargets(schedule);
        when(autoEnrollmentBatchLaunchService.launch(semester, schedule))
            .thenThrow(new StudentEnrollmentException(
                StudentEnrollmentErrorCode.AUTO_ENROLLMENT_BATCH_ALREADY_COMPLETED
            ));

        scheduler.runPreApply();

        verify(autoEnrollmentBatchLaunchService).launch(semester, schedule);
    }

    @Test
    void skipsAlreadyRunningJobWithoutFailure() {
        SemesterSchedule schedule = schedule(
            LocalDateTime.of(2026, 6, 28, 9, 0),
            false
        );
        stubTargets(schedule);
        when(autoEnrollmentBatchLaunchService.launch(semester, schedule))
            .thenThrow(new StudentEnrollmentException(
                StudentEnrollmentErrorCode.AUTO_ENROLLMENT_BATCH_ALREADY_RUNNING
            ));

        scheduler.runPreApply();

        verify(autoEnrollmentBatchLaunchService).launch(semester, schedule);
    }

    private SemesterSchedule schedule(LocalDateTime startAt, boolean alwaysOpen) {
        SemesterSchedule schedule = org.mockito.Mockito.mock(SemesterSchedule.class);
        when(schedule.getType()).thenReturn(SemesterScheduleType.ENROLLMENT);
        when(schedule.isAlwaysOpen()).thenReturn(alwaysOpen);
        if (!alwaysOpen) {
            when(schedule.getStartAt()).thenReturn(startAt);
        }
        lenient().when(schedule.getSemester()).thenReturn(semester);
        return schedule;
    }

    private void stubTargets(SemesterSchedule... schedules) {
        when(semesterScheduleRepository.findPreApplyTargets(
            SemesterScheduleType.ENROLLMENT,
            TOMORROW_START,
            DAY_AFTER_TOMORROW_START
        )).thenReturn(List.of(schedules));
    }

    private AutoEnrollmentBatchLaunchResponse completedResponse() {
        return new AutoEnrollmentBatchLaunchResponse(
            1L,
            "autoEnrollmentPreApplyJob",
            "COMPLETED",
            1L,
            2026,
            SemesterTerm.FIRST,
            LocalDateTime.of(2026, 6, 28, 9, 0),
            TODAY,
            1,
            1,
            0,
            0,
            Map.of("APPLIED", 1L)
        );
    }
}
