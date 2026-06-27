package com.example.CampusFlowServer.domain.student.enrollment.service;

import com.example.CampusFlowServer.domain.enrollment.entity.WishCourse;
import com.example.CampusFlowServer.domain.enrollment.enums.WishAutoApplyResult;
import com.example.CampusFlowServer.domain.enrollment.repository.WishCourseRepository;
import com.example.CampusFlowServer.domain.semester.entity.Semester;
import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import com.example.CampusFlowServer.domain.semester.repository.SemesterRepository;
import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentApplyOneResult;
import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentApplyResponse;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentErrorCode;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutoEnrollmentService {

    private final SemesterRepository semesterRepository;
    private final WishCourseRepository wishCourseRepository;
    private final AutoEnrollmentCommandService autoEnrollmentCommandService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AutoEnrollmentApplyResponse applyAutoEnrollments(Integer year, SemesterTerm term) {
        if (year == null) {
            throw new StudentEnrollmentException(StudentEnrollmentErrorCode.REQUIRED_YEAR);
        }
        if (term == null) {
            throw new StudentEnrollmentException(StudentEnrollmentErrorCode.REQUIRED_TERM);
        }

        Semester semester = semesterRepository.findByYearAndTerm(year, term)
            .orElseThrow(() -> new StudentEnrollmentException(
                StudentEnrollmentErrorCode.SEMESTER_NOT_FOUND
            ));
        List<WishCourse> targets = wishCourseRepository.findBySemesterIdAndAutoApplyTrueAndResult(
            semester.getId(),
            WishAutoApplyResult.DONE
        );

        int appliedCount = 0;
        for (WishCourse target : targets) {
            AutoEnrollmentApplyOneResult result = autoEnrollmentCommandService.applyOne(
                target.getId(),
                target.getCourseOffering().getId()
            );
            if (result.applied()) {
                appliedCount++;
            }
        }
        return new AutoEnrollmentApplyResponse(
            targets.size(),
            appliedCount,
            targets.size() - appliedCount
        );
    }
}
