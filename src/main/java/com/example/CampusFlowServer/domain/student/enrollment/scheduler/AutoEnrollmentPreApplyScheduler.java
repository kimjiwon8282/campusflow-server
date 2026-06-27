package com.example.CampusFlowServer.domain.student.enrollment.scheduler;

import com.example.CampusFlowServer.domain.semester.entity.Semester;
import com.example.CampusFlowServer.domain.semester.entity.SemesterSchedule;
import com.example.CampusFlowServer.domain.semester.enums.SemesterScheduleType;
import com.example.CampusFlowServer.domain.semester.repository.SemesterScheduleRepository;
import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentBatchLaunchResponse;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentErrorCode;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentException;
import com.example.CampusFlowServer.domain.student.enrollment.service.AutoEnrollmentBatchLaunchService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "campusflow.auto-enrollment.pre-apply.scheduler",
    name = "enabled",
    havingValue = "true"
)
public class AutoEnrollmentPreApplyScheduler {

    private final SemesterScheduleRepository semesterScheduleRepository;
    private final AutoEnrollmentBatchLaunchService autoEnrollmentBatchLaunchService;
    @Qualifier("autoEnrollmentSchedulerClock")
    private final Clock clock;

    @Scheduled(
        cron = "${campusflow.auto-enrollment.pre-apply.scheduler.cron:0 0 2 * * *}",
        zone = "${campusflow.auto-enrollment.pre-apply.scheduler.zone:Asia/Seoul}"
    )
    public void runPreApply() {
        LocalDate today = LocalDate.now(clock);
        LocalDate targetDate = today.plusDays(1);
        LocalDateTime from = targetDate.atStartOfDay();
        LocalDateTime to = targetDate.plusDays(1).atStartOfDay();
        log.info(
            "Auto-enrollment D-1 scheduler started: startedAt={}, businessDate={}",
            ZonedDateTime.now(clock),
            today
        );

        List<SemesterSchedule> schedules = semesterScheduleRepository.findPreApplyTargets(
            SemesterScheduleType.ENROLLMENT,
            from,
            to
        );
        log.info("Auto-enrollment D-1 scheduler found {} target semester(s)", schedules.size());

        for (SemesterSchedule schedule : schedules) {
            if (!isD1Target(schedule, targetDate)) {
                continue;
            }
            launch(schedule, today);
        }
    }

    private boolean isD1Target(SemesterSchedule schedule, LocalDate targetDate) {
        return schedule != null
            && SemesterScheduleType.ENROLLMENT.equals(schedule.getType())
            && !schedule.isAlwaysOpen()
            && schedule.getStartAt() != null
            && targetDate.equals(schedule.getStartAt().toLocalDate());
    }

    private void launch(SemesterSchedule schedule, LocalDate businessDate) {
        Semester semester = schedule.getSemester();
        log.info(
            "Launching auto-enrollment pre-apply batch: semesterId={}, academicYear={}, "
                + "term={}, enrollmentStartAt={}, businessDate={}",
            semester.getId(),
            semester.getYear(),
            semester.getTerm(),
            schedule.getStartAt(),
            businessDate
        );
        try {
            AutoEnrollmentBatchLaunchResponse response = autoEnrollmentBatchLaunchService.launch(
                semester,
                schedule
            );
            if ("FAILED".equals(response.status())) {
                log.error(
                    "Auto-enrollment pre-apply batch failed: jobExecutionId={}, semesterId={}, "
                        + "appliedCount={}, skippedCount={}, failedCount={}",
                    response.jobExecutionId(),
                    semester.getId(),
                    response.appliedCount(),
                    response.skippedCount(),
                    response.failedCount()
                );
            } else {
                log.info(
                    "Auto-enrollment pre-apply batch finished: jobExecutionId={}, status={}, "
                        + "semesterId={}, appliedCount={}, skippedCount={}, failedCount={}",
                    response.jobExecutionId(),
                    response.status(),
                    semester.getId(),
                    response.appliedCount(),
                    response.skippedCount(),
                    response.failedCount()
                );
            }
        } catch (StudentEnrollmentException exception) {
            if (StudentEnrollmentErrorCode.AUTO_ENROLLMENT_BATCH_ALREADY_COMPLETED.equals(
                exception.getErrorCode()
            )) {
                log.info(
                    "Skipping completed auto-enrollment batch: semesterId={}, "
                        + "enrollmentStartAt={}, businessDate={}",
                    semester.getId(),
                    schedule.getStartAt(),
                    businessDate
                );
                return;
            }
            if (StudentEnrollmentErrorCode.AUTO_ENROLLMENT_BATCH_ALREADY_RUNNING.equals(
                exception.getErrorCode()
            )) {
                log.info(
                    "Skipping running auto-enrollment batch: semesterId={}, "
                        + "enrollmentStartAt={}, businessDate={}",
                    semester.getId(),
                    schedule.getStartAt(),
                    businessDate
                );
                return;
            }
            log.error(
                "Unexpected auto-enrollment scheduler failure: semesterId={}, "
                    + "enrollmentStartAt={}, businessDate={}",
                semester.getId(),
                schedule.getStartAt(),
                businessDate,
                exception
            );
        } catch (RuntimeException exception) {
            log.error(
                "Unexpected auto-enrollment scheduler failure: semesterId={}, "
                    + "enrollmentStartAt={}, businessDate={}",
                semester.getId(),
                schedule.getStartAt(),
                businessDate,
                exception
            );
        }
    }
}
