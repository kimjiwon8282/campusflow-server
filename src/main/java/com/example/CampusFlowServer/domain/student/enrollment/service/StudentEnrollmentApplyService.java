package com.example.CampusFlowServer.domain.student.enrollment.service;

import com.example.CampusFlowServer.domain.enrollment.enums.EnrollmentStatus;
import com.example.CampusFlowServer.domain.enrollment.repository.EnrollmentRepository;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentApplyRequest;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentApplyResponse;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentErrorCode;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentEnrollmentApplyService {

    private final StudentEnrollmentPreValidationService preValidationService;
    private final StudentEnrollmentLockCommandService lockCommandService;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public StudentEnrollmentApplyResponse apply(
        Long memberId,
        StudentEnrollmentApplyRequest request
    ) {
        long applyStartedAt = System.nanoTime();
        if (request == null || request.courseOfferingId() == null) {
            throw new StudentEnrollmentException(
                StudentEnrollmentErrorCode.REQUIRED_COURSE_OFFERING
            );
        }

        long preValidationStartedAt = System.nanoTime();
        EnrollmentPreValidationResult preValidation = preValidationService.validate(
            memberId,
            request.courseOfferingId()
        );
        logDebugElapsed("student enrollment pre-validation", preValidationStartedAt);

        long commandStartedAt = System.nanoTime();
        EnrollmentApplyCoreResult coreResult = lockCommandService.applyWithLock(preValidation);
        logDebugElapsed("student enrollment command transaction including commit", commandStartedAt);

        Integer waitNo = null;
        if (EnrollmentStatus.WAITING.equals(coreResult.status())) {
            long waitNoStartedAt = System.nanoTime();
            waitNo = calculateWaitNoByCount(coreResult);
            logDebugElapsed("student enrollment wait number count", waitNoStartedAt);
        }
        logDebugElapsed("student enrollment apply total", applyStartedAt);

        return new StudentEnrollmentApplyResponse(
            coreResult.enrollmentId(),
            coreResult.courseOfferingId(),
            coreResult.subjectCode(),
            coreResult.subjectName(),
            coreResult.status().name(),
            coreResult.source().name(),
            waitNo,
            EnrollmentStatus.ENROLLED.equals(coreResult.status())
                ? "수강신청이 완료되었습니다."
                : "정원이 마감되어 대기 신청되었습니다."
        );
    }

    private int calculateWaitNoByCount(EnrollmentApplyCoreResult coreResult) {
        long waitNo = enrollmentRepository.countWaitingPosition(
            coreResult.courseOfferingId(),
            EnrollmentStatus.WAITING,
            coreResult.enrollmentId()
        );
        return Math.toIntExact(waitNo);
    }

    private void logDebugElapsed(String label, long startedAt) {
        if (log.isDebugEnabled()) {
            log.debug("{} took {} ms", label, elapsedMillis(startedAt));
        }
    }

    private double elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000.0;
    }
}
