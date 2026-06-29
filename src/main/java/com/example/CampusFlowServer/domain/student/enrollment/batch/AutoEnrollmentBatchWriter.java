package com.example.CampusFlowServer.domain.student.enrollment.batch;

import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentApplyOneResult;
import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentApplyOneStatus;
import com.example.CampusFlowServer.domain.student.enrollment.service.AutoEnrollmentCommandService;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class AutoEnrollmentBatchWriter
    implements ItemWriter<AutoEnrollmentBatchItem>, StepExecutionListener {

    public static final String TARGET_COUNT = "autoEnrollment.targetCount";
    public static final String APPLIED_COUNT = "autoEnrollment.appliedCount";
    public static final String SKIPPED_COUNT = "autoEnrollment.skippedCount";
    public static final String FAILED_COUNT = "autoEnrollment.failedCount";
    public static final String STATUS_COUNT_PREFIX = "autoEnrollment.status.";

    private final AutoEnrollmentCommandService autoEnrollmentCommandService;
    private final Map<AutoEnrollmentApplyOneStatus, Long> statusCounts =
        new EnumMap<>(AutoEnrollmentApplyOneStatus.class);

    private long targetCount;
    private long appliedCount;
    private long skippedCount;
    private long failedCount;
    @Override
    public void write(Chunk<? extends AutoEnrollmentBatchItem> chunk) {
        for (AutoEnrollmentBatchItem item : chunk) {
            targetCount++;
            try {
                AutoEnrollmentApplyOneResult result = autoEnrollmentCommandService.applyOne(//실제 자동 신청은 Command에 위임함
                    item.wishCourseId(),
                    item.courseOfferingId()
                );
                statusCounts.merge(result.status(), 1L, Long::sum);
                if (result.applied()) {
                    appliedCount++;
                } else {
                    skippedCount++;
                }
            } catch (RuntimeException exception) {
                failedCount++;
                throw exception;
            }
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        ExecutionContext context = stepExecution.getExecutionContext();
        context.putLong(TARGET_COUNT, targetCount);
        context.putLong(APPLIED_COUNT, appliedCount);
        context.putLong(SKIPPED_COUNT, skippedCount);
        context.putLong(FAILED_COUNT, failedCount);
        statusCounts.forEach((status, count) ->
            context.putLong(STATUS_COUNT_PREFIX + status.name(), count)
        );
        return stepExecution.getExitStatus();
    }
}
