package com.example.CampusFlowServer.domain.student.semesterschedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.CampusFlowServer.domain.semester.entity.Semester;
import com.example.CampusFlowServer.domain.semester.entity.SemesterSchedule;
import com.example.CampusFlowServer.domain.semester.enums.SemesterScheduleType;
import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import com.example.CampusFlowServer.domain.semester.repository.SemesterRepository;
import com.example.CampusFlowServer.domain.semester.repository.SemesterScheduleRepository;
import com.example.CampusFlowServer.domain.student.semesterschedule.dto.StudentSemesterScheduleStatusRequest;
import com.example.CampusFlowServer.domain.student.semesterschedule.dto.StudentSemesterScheduleStatusResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentSemesterScheduleServiceTest {

    private static final Long SEMESTER_ID = 1L;
    private static final StudentSemesterScheduleStatusRequest REQUEST =
        new StudentSemesterScheduleStatusRequest(2026, SemesterTerm.FIRST);

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private SemesterScheduleRepository semesterScheduleRepository;

    private StudentSemesterScheduleService service;
    private Semester semester;

    @BeforeEach
    void setUp() {
        service = new StudentSemesterScheduleService(
            semesterRepository,
            semesterScheduleRepository
        );
        semester = Semester.create(2026, SemesterTerm.FIRST, "2026학년도 1학기", 18);
        ReflectionTestUtils.setField(semester, "id", SEMESTER_ID);
        when(semesterRepository.findByYearAndTerm(2026, SemesterTerm.FIRST))
            .thenReturn(Optional.of(semester));
    }

    @Test
    void returnsEnrollmentOpenDuringEnrollmentPeriod() {
        stubSchedule(SemesterScheduleType.ENROLLMENT, openSchedule(SemesterScheduleType.ENROLLMENT));

        StudentSemesterScheduleStatusResponse response = service.getStatus(REQUEST);

        assertThat(response.enrollmentOpen()).isTrue();
        assertThat(response.enrollmentMessage()).isEqualTo("수강신청 기간입니다.");
    }

    @Test
    void returnsEnrollmentClosedOutsideEnrollmentPeriod() {
        stubSchedule(SemesterScheduleType.ENROLLMENT, closedSchedule(SemesterScheduleType.ENROLLMENT));

        StudentSemesterScheduleStatusResponse response = service.getStatus(REQUEST);

        assertThat(response.enrollmentOpen()).isFalse();
        assertThat(response.enrollmentMessage()).isEqualTo("수강신청 기간이 아닙니다.");
    }

    @Test
    void returnsWishOpenDuringWishlistPeriod() {
        stubNoSchedule(SemesterScheduleType.ENROLLMENT);
        stubSchedule(SemesterScheduleType.WISHLIST, openSchedule(SemesterScheduleType.WISHLIST));

        StudentSemesterScheduleStatusResponse response = service.getStatus(REQUEST);

        assertThat(response.wishOpen()).isTrue();
        assertThat(response.wishMessage()).isEqualTo("희망과목 담기 기간입니다.");
    }

    @Test
    void returnsWishClosedOutsideWishlistPeriod() {
        stubNoSchedule(SemesterScheduleType.ENROLLMENT);
        stubSchedule(SemesterScheduleType.WISHLIST, closedSchedule(SemesterScheduleType.WISHLIST));

        StudentSemesterScheduleStatusResponse response = service.getStatus(REQUEST);

        assertThat(response.wishOpen()).isFalse();
        assertThat(response.wishMessage()).isEqualTo("희망과목 담기 기간이 아닙니다.");
    }

    @Test
    void returnsClosedWhenSchedulesDoNotExist() {
        StudentSemesterScheduleStatusResponse response = service.getStatus(REQUEST);

        assertThat(response.enrollmentOpen()).isFalse();
        assertThat(response.wishOpen()).isFalse();
    }

    @Test
    void returnsOpenWhenAlwaysOpenIsTrue() {
        SemesterSchedule enrollment = SemesterSchedule.create(
            semester,
            SemesterScheduleType.ENROLLMENT,
            null,
            null,
            null,
            true
        );
        SemesterSchedule wishlist = SemesterSchedule.create(
            semester,
            SemesterScheduleType.WISHLIST,
            null,
            null,
            null,
            true
        );
        stubSchedule(SemesterScheduleType.ENROLLMENT, enrollment);
        stubSchedule(SemesterScheduleType.WISHLIST, wishlist);

        StudentSemesterScheduleStatusResponse response = service.getStatus(REQUEST);

        assertThat(response.enrollmentOpen()).isTrue();
        assertThat(response.wishOpen()).isTrue();
    }

    private SemesterSchedule openSchedule(SemesterScheduleType type) {
        LocalDateTime now = LocalDateTime.now();
        return SemesterSchedule.create(
            semester,
            type,
            now.minusMinutes(1),
            now.plusMinutes(1),
            null,
            false
        );
    }

    private SemesterSchedule closedSchedule(SemesterScheduleType type) {
        LocalDateTime now = LocalDateTime.now();
        return SemesterSchedule.create(
            semester,
            type,
            now.minusMinutes(2),
            now.minusMinutes(1),
            null,
            false
        );
    }

    private void stubSchedule(SemesterScheduleType type, SemesterSchedule schedule) {
        when(semesterScheduleRepository.findBySemesterIdAndType(SEMESTER_ID, type))
            .thenReturn(Optional.of(schedule));
    }

    private void stubNoSchedule(SemesterScheduleType type) {
        when(semesterScheduleRepository.findBySemesterIdAndType(SEMESTER_ID, type))
            .thenReturn(Optional.empty());
    }
}
