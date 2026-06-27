package com.example.CampusFlowServer.domain.student.enrollment.service;

import com.example.CampusFlowServer.domain.semester.entity.Semester;
import com.example.CampusFlowServer.domain.semester.entity.SemesterSchedule;
import com.example.CampusFlowServer.domain.semester.enums.SemesterScheduleType;
import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import com.example.CampusFlowServer.domain.semester.repository.SemesterRepository;
import com.example.CampusFlowServer.domain.semester.repository.SemesterScheduleRepository;
import com.example.CampusFlowServer.domain.student.enrollment.batch.AutoEnrollmentBatchConfig;
import com.example.CampusFlowServer.domain.student.enrollment.batch.AutoEnrollmentBatchWriter;
import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentApplyOneStatus;
import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentBatchLaunchResponse;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentErrorCode;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoEnrollmentBatchLaunchService {

    private final JobOperator jobOperator;
    @Qualifier(AutoEnrollmentBatchConfig.JOB_NAME)
    private final Job autoEnrollmentPreApplyJob;
    private final SemesterRepository semesterRepository;
    private final SemesterScheduleRepository semesterScheduleRepository;

    public AutoEnrollmentBatchLaunchResponse launch(Integer year, SemesterTerm term) {
        validateRequired(year, term);
        Semester semester = semesterRepository.findByYearAndTerm(year, term)
            .orElseThrow(() -> new StudentEnrollmentException(
                StudentEnrollmentErrorCode.SEMESTER_NOT_FOUND
            ));
        SemesterSchedule schedule = semesterScheduleRepository
            .findBySemesterIdAndType(semester.getId(), SemesterScheduleType.ENROLLMENT)
            .orElseThrow(() -> new StudentEnrollmentException(
                StudentEnrollmentErrorCode.AUTO_ENROLLMENT_SCHEDULE_NOT_FOUND
            ));
        return launch(semester, schedule);
    }

    public AutoEnrollmentBatchLaunchResponse launch(
        Semester semester,
        SemesterSchedule schedule
    ) {
        if (schedule.isAlwaysOpen()) {
            throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.AUTO_ENROLLMENT_ALWAYS_OPEN_UNSUPPORTED
            );
        }
        LocalDateTime enrollmentStartAt = schedule.getStartAt();
        if (enrollmentStartAt == null) {
            throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.AUTO_ENROLLMENT_START_AT_REQUIRED
            );
        }
        LocalDate businessDate = enrollmentStartAt.toLocalDate().minusDays(1);
        JobParameters jobParameters = new JobParametersBuilder()
            .addLong("semesterId", semester.getId())
            .addLong("academicYear", semester.getYear().longValue())
            .addString("term", semester.getTerm().name())
            .addLocalDateTime("enrollmentStartAt", enrollmentStartAt)
            .addLocalDate("businessDate", businessDate)
            .toJobParameters();

        try {
            JobExecution execution = jobOperator.start(
                autoEnrollmentPreApplyJob,
                jobParameters
            );
            AutoEnrollmentBatchLaunchResponse response = toResponse(
                execution,
                semester,
                enrollmentStartAt,
                businessDate
            );
            log.info(
                "Auto-enrollment pre-apply batch finished: jobExecutionId={}, status={}, "
                    + "semesterId={}, enrollmentStartAt={}, businessDate={}, applied={}, "
                    + "skipped={}, failed={}",
                response.jobExecutionId(),
                response.status(),
                response.semesterId(),
                response.enrollmentStartAt(),
                response.businessDate(),
                response.appliedCount(),
                response.skippedCount(),
                response.failedCount()
            );
            return response;
        } catch (JobInstanceAlreadyCompleteException exception) {
            throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.AUTO_ENROLLMENT_BATCH_ALREADY_COMPLETED
            );
        } catch (JobExecutionAlreadyRunningException exception) {
            throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.AUTO_ENROLLMENT_BATCH_ALREADY_RUNNING
            );
        } catch (JobRestartException | InvalidJobParametersException exception) {
            throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.AUTO_ENROLLMENT_BATCH_LAUNCH_FAILED
            );
        }
    }

    private AutoEnrollmentBatchLaunchResponse toResponse(
        JobExecution execution,
        Semester semester,
        LocalDateTime enrollmentStartAt,
        LocalDate businessDate
    ) {
        StepExecution stepExecution = execution.getStepExecutions().stream()
            .filter(step -> AutoEnrollmentBatchConfig.STEP_NAME.equals(step.getStepName()))
            .findFirst()
            .orElse(null);
        ExecutionContext context = stepExecution == null
            ? new ExecutionContext()
            : stepExecution.getExecutionContext();
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        Arrays.stream(AutoEnrollmentApplyOneStatus.values()).forEach(status -> {
            long count = context.getLong(
                AutoEnrollmentBatchWriter.STATUS_COUNT_PREFIX + status.name(),
                0L
            );
            if (count > 0) {
                statusCounts.put(status.name(), count);
            }
        });

        return new AutoEnrollmentBatchLaunchResponse(
            execution.getId(),
            execution.getJobInstance().getJobName(),
            execution.getStatus().name(),
            semester.getId(),
            semester.getYear(),
            semester.getTerm(),
            enrollmentStartAt,
            businessDate,
            context.getLong(AutoEnrollmentBatchWriter.TARGET_COUNT, 0L),
            context.getLong(AutoEnrollmentBatchWriter.APPLIED_COUNT, 0L),
            context.getLong(AutoEnrollmentBatchWriter.SKIPPED_COUNT, 0L),
            context.getLong(AutoEnrollmentBatchWriter.FAILED_COUNT, 0L),
            Map.copyOf(statusCounts)
        );
    }

    private void validateRequired(Integer year, SemesterTerm term) {
        if (year == null) {
            throw new StudentEnrollmentException(StudentEnrollmentErrorCode.REQUIRED_YEAR);
        }
        if (term == null) {
            throw new StudentEnrollmentException(StudentEnrollmentErrorCode.REQUIRED_TERM);
        }
    }
}
