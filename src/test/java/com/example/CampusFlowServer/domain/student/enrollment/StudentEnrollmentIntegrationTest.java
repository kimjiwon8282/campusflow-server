package com.example.CampusFlowServer.domain.student.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.CampusFlowServer.domain.course.entity.CourseOffering;
import com.example.CampusFlowServer.domain.course.entity.CourseTime;
import com.example.CampusFlowServer.domain.course.entity.Subject;
import com.example.CampusFlowServer.domain.course.enums.CourseOfferingStatus;
import com.example.CampusFlowServer.domain.course.enums.DayOfWeek;
import com.example.CampusFlowServer.domain.course.enums.SubjectCategory;
import com.example.CampusFlowServer.domain.course.repository.CourseOfferingRepository;
import com.example.CampusFlowServer.domain.course.repository.CourseTimeRepository;
import com.example.CampusFlowServer.domain.course.repository.SubjectRepository;
import com.example.CampusFlowServer.domain.enrollment.entity.Enrollment;
import com.example.CampusFlowServer.domain.enrollment.entity.CompletedCourse;
import com.example.CampusFlowServer.domain.enrollment.entity.CourseAllowedGrade;
import com.example.CampusFlowServer.domain.enrollment.entity.CoursePrerequisite;
import com.example.CampusFlowServer.domain.enrollment.entity.WishCourse;
import com.example.CampusFlowServer.domain.enrollment.enums.EnrollmentSource;
import com.example.CampusFlowServer.domain.enrollment.enums.EnrollmentStatus;
import com.example.CampusFlowServer.domain.enrollment.enums.WishAutoApplyResult;
import com.example.CampusFlowServer.domain.enrollment.repository.CompletedCourseRepository;
import com.example.CampusFlowServer.domain.enrollment.repository.CourseAllowedGradeRepository;
import com.example.CampusFlowServer.domain.enrollment.repository.CoursePrerequisiteRepository;
import com.example.CampusFlowServer.domain.enrollment.repository.EnrollmentRepository;
import com.example.CampusFlowServer.domain.enrollment.repository.WishCourseRepository;
import com.example.CampusFlowServer.domain.member.entity.Department;
import com.example.CampusFlowServer.domain.member.entity.Member;
import com.example.CampusFlowServer.domain.member.entity.ProfessorProfile;
import com.example.CampusFlowServer.domain.member.entity.StaffProfile;
import com.example.CampusFlowServer.domain.member.entity.StudentProfile;
import com.example.CampusFlowServer.domain.member.enums.AcademicStatus;
import com.example.CampusFlowServer.domain.member.enums.DepartmentType;
import com.example.CampusFlowServer.domain.member.enums.MemberRole;
import com.example.CampusFlowServer.domain.member.repository.DepartmentRepository;
import com.example.CampusFlowServer.domain.member.repository.MemberRepository;
import com.example.CampusFlowServer.domain.member.repository.ProfessorProfileRepository;
import com.example.CampusFlowServer.domain.member.repository.StaffProfileRepository;
import com.example.CampusFlowServer.domain.member.repository.StudentProfileRepository;
import com.example.CampusFlowServer.domain.semester.entity.Semester;
import com.example.CampusFlowServer.domain.semester.entity.SemesterSchedule;
import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import com.example.CampusFlowServer.domain.semester.enums.SemesterScheduleType;
import com.example.CampusFlowServer.domain.semester.repository.SemesterRepository;
import com.example.CampusFlowServer.domain.semester.repository.SemesterScheduleRepository;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StudentEnrollmentIntegrationTest {

    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String PASSWORD = "password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private ProfessorProfileRepository professorProfileRepository;

    @Autowired
    private StaffProfileRepository staffProfileRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private SemesterScheduleRepository semesterScheduleRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private CourseOfferingRepository courseOfferingRepository;

    @Autowired
    private CourseTimeRepository courseTimeRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private WishCourseRepository wishCourseRepository;

    @Autowired
    private CourseAllowedGradeRepository courseAllowedGradeRepository;

    @Autowired
    private CoursePrerequisiteRepository coursePrerequisiteRepository;

    @Autowired
    private CompletedCourseRepository completedCourseRepository;

    @Test
    void cancellingEnrolledEnrollmentChangesStatusToCancelled() throws Exception {
        EnrollmentFixture fixture = saveFixture("cancel-enrolled");
        saveEnrollmentSchedule(fixture.semester(), true);
        Enrollment enrollment = saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.ENROLLED,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiDelete("/api/v1/student/enrollments/" + enrollment.getId())
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(enrollmentRepository.findById(enrollment.getId()).orElseThrow().getStatus())
            .isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    void cancellingWaitingEnrollmentChangesStatusToCancelled() throws Exception {
        EnrollmentFixture fixture = saveFixture("cancel-waiting");
        saveEnrollmentSchedule(fixture.semester(), true);
        Enrollment enrollment = saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.WAITING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiDelete("/api/v1/student/enrollments/" + enrollment.getId())
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(enrollmentRepository.findById(enrollment.getId()).orElseThrow().getStatus())
            .isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    void cancellingOthersEnrollmentReturnsForbidden() throws Exception {
        EnrollmentFixture fixture = saveFixture("cancel-owner");
        saveEnrollmentSchedule(fixture.semester(), true);
        Enrollment enrollment = saveEnrollment(
            fixture.otherStudentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.ENROLLED,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiDelete("/api/v1/student/enrollments/" + enrollment.getId())
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ENROLLMENT_015"));
    }

    @Test
    void cancellingOutsideEnrollmentPeriodFails() throws Exception {
        EnrollmentFixture fixture = saveFixture("cancel-closed");
        saveEnrollmentSchedule(fixture.semester(), false);
        Enrollment enrollment = saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.ENROLLED,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiDelete("/api/v1/student/enrollments/" + enrollment.getId())
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ENROLLMENT_008"));
    }

    @Test
    void cancellingEnrolledEnrollmentPromotesFirstWaitingEnrollment() throws Exception {
        EnrollmentFixture fixture = saveFixture("promote");
        saveEnrollmentSchedule(fixture.semester(), true);
        Enrollment enrolled = saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.ENROLLED,
            null
        );
        Enrollment waiting = saveEnrollment(
            fixture.otherStudentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.WAITING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiDelete("/api/v1/student/enrollments/" + enrolled.getId())
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.promotedEnrollmentId").value(waiting.getId()));

        assertThat(enrollmentRepository.findById(waiting.getId()).orElseThrow().getStatus())
            .isEqualTo(EnrollmentStatus.ENROLLED);
    }

    @Test
    void cancellingMiddleWaitingEnrollmentRecalculatesFollowingWaitNoOnRead() throws Exception {
        EnrollmentFixture fixture = saveFixture("wait-cancel");
        saveEnrollmentSchedule(fixture.semester(), true);
        StudentProfile thirdStudentProfile = saveStudentProfile(
            "enrollment-third-student-wait-cancel",
            "Third Student wait-cancel",
            "ETS-wait-cancel",
            fixture.courseOffering().getSubject().getDepartment()
        );
        saveEnrollment(fixture.studentProfile(), fixture.semester(), fixture.courseOffering(), EnrollmentStatus.ENROLLED, null);
        Enrollment middleWaiting = saveEnrollment(
            fixture.otherStudentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.WAITING,
            null
        );
        Enrollment followingWaiting = saveEnrollment(
            thirdStudentProfile,
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.WAITING,
            null
        );
        Cookie middleToken = login(fixture.otherStudentProfile().getMember().getLoginId());
        Cookie followingToken = login(thirdStudentProfile.getMember().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiDelete("/api/v1/student/enrollments/" + middleWaiting.getId())
                .cookie(middleToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue()))
            .andExpect(status().isOk());

        mockMvc.perform(apiGet("/api/v1/student/enrollments")
                .cookie(followingToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.waitingCourses[0].enrollmentId").value(followingWaiting.getId()))
            .andExpect(jsonPath("$.waitingCourses[0].waitNo").value(1));
    }

    @Test
    void reAppliedWaitingEnrollmentIsOrderedByAppliedAtAfterCurrentWaiters()
        throws Exception {
        EnrollmentFixture fixture = saveFixture("reapply-wait-order");
        saveEnrollmentSchedule(fixture.semester(), true);
        CourseOffering fullOffering = saveOffering(
            fixture,
            "ENR102-reapply-wait-order",
            "Networks",
            1,
            DayOfWeek.TUE
        );
        StudentProfile seatHolder = saveStudentProfile(
            "enrollment-seat-holder-reapply-wait-order",
            "Seat Holder reapply-wait-order",
            "ESH-reapply-wait-order",
            fixture.courseOffering().getSubject().getDepartment()
        );
        saveEnrollment(seatHolder, fixture.semester(), fullOffering, EnrollmentStatus.ENROLLED, null);

        Cookie firstToken = login(fixture.student().getLoginId());
        Cookie secondToken = login(fixture.otherStudentProfile().getMember().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        MvcResult firstApply = mockMvc.perform(apiPost("/api/v1/student/enrollments")
                .cookie(firstToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fullOffering.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.waitNo").value(1))
            .andReturn();
        Long firstEnrollmentId = ((Number) com.jayway.jsonpath.JsonPath
            .read(firstApply.getResponse().getContentAsString(), "$.enrollmentId"))
            .longValue();

        mockMvc.perform(apiDelete("/api/v1/student/enrollments/" + firstEnrollmentId)
                .cookie(firstToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue()))
            .andExpect(status().isOk());

        mockMvc.perform(apiPost("/api/v1/student/enrollments")
                .cookie(secondToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fullOffering.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.waitNo").value(1));

        mockMvc.perform(apiPost("/api/v1/student/enrollments")
                .cookie(firstToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fullOffering.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.waitNo").value(2));

        mockMvc.perform(apiGet("/api/v1/student/enrollments")
                .cookie(secondToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.waitingCourses[0].waitNo").value(1));

        mockMvc.perform(apiGet("/api/v1/student/enrollments")
                .cookie(firstToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.waitingCourses[0].waitNo").value(2));
    }

    @Test
    void waitNumberStoredValueIsIgnoredWhenCalculatingWaitNo() throws Exception {
        EnrollmentFixture fixture = saveFixture("wait-number-ignore");
        Enrollment firstWaiting = saveEnrollmentAt(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.WAITING,
            99,
            LocalDateTime.now().minusMinutes(2)
        );
        Enrollment secondWaiting = saveEnrollmentAt(
            fixture.otherStudentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.WAITING,
            1,
            LocalDateTime.now().minusMinutes(1)
        );
        Cookie firstToken = login(fixture.student().getLoginId());
        Cookie secondToken = login(fixture.otherStudentProfile().getMember().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/enrollments")
                .cookie(firstToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.waitingCourses[0].enrollmentId").value(firstWaiting.getId()))
            .andExpect(jsonPath("$.waitingCourses[0].waitNo").value(1));

        mockMvc.perform(apiGet("/api/v1/student/enrollments")
                .cookie(secondToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.waitingCourses[0].enrollmentId").value(secondWaiting.getId()))
            .andExpect(jsonPath("$.waitingCourses[0].waitNo").value(2));
    }

    @Test
    void waitingCourseTimeDoesNotCauseTimeConflictInCourseSearch() throws Exception {
        EnrollmentFixture fixture = saveFixture("waiting-no-conflict");
        CourseOffering sameTimeOffering = saveOfferingWithTime(
            fixture,
            "ENR102-waiting-no-conflict",
            "Parallel Systems",
            30,
            DayOfWeek.MON,
            1,
            2
        );
        saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.WAITING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/enrollments/courses")
                .cookie(accessToken)
                .param("mode", "direct")
                .param("year", "2026")
                .param("term", "FIRST")
                .param("subjectName", "Parallel Systems"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courseOfferingId").value(sameTimeOffering.getId()))
            .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void enrolledCourseTimeStillCausesTimeConflictInCourseSearch() throws Exception {
        EnrollmentFixture fixture = saveFixture("enrolled-conflict");
        CourseOffering sameTimeOffering = saveOfferingWithTime(
            fixture,
            "ENR102-enrolled-conflict",
            "Parallel Systems",
            30,
            DayOfWeek.MON,
            1,
            2
        );
        saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.ENROLLED,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/enrollments/courses")
                .cookie(accessToken)
                .param("mode", "direct")
                .param("year", "2026")
                .param("term", "FIRST")
                .param("subjectName", "Parallel Systems"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courseOfferingId").value(sameTimeOffering.getId()))
            .andExpect(jsonPath("$[0].status").value("TIME_CONFLICT"));
    }


    @Test
    void applyingDoneWishCoursesCreatesAutoEnrollmentAndSkipsActiveDuplicate() throws Exception {
        EnrollmentFixture fixture = saveFixture("auto-apply");
        CourseOffering duplicateOffering = saveOffering(
            fixture,
            "ENR102-auto-apply",
            "Databases",
            30,
            DayOfWeek.TUE
        );
        saveWishAuto(fixture.studentProfile(), fixture.semester(), fixture.courseOffering(), WishAutoApplyResult.DONE);
        saveWishAuto(fixture.studentProfile(), fixture.semester(), duplicateOffering, WishAutoApplyResult.DONE);
        saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            duplicateOffering,
            EnrollmentStatus.ENROLLED,
            null
        );
        Cookie staffToken = login(fixture.staff().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/staff/auto-enrollments/apply")
                .cookie(staffToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.targetCount").value(2))
            .andExpect(jsonPath("$.createdCount").value(1))
            .andExpect(jsonPath("$.skippedCount").value(1));

        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndSemesterIdAndStatusIn(
            fixture.studentProfile().getId(),
            fixture.semester().getId(),
            List.of(EnrollmentStatus.ENROLLED)
        );
        assertThat(enrollments)
            .filteredOn(enrollment -> enrollment.getCourseOffering().getId().equals(
                fixture.courseOffering().getId()
            ))
            .singleElement()
            .satisfies(enrollment -> assertThat(enrollment.getSource())
                .isEqualTo(EnrollmentSource.AUTO));
        assertThat(enrollments)
            .filteredOn(enrollment -> enrollment.getCourseOffering().getId().equals(
                duplicateOffering.getId()
            ))
            .hasSize(1);
    }

    @Test
    void autoEnrollmentSourceIsReturnedInMyEnrollmentsAndCreditsRecalculateAfterCancel()
        throws Exception {
        EnrollmentFixture fixture = saveFixture("auto-read");
        saveEnrollmentSchedule(fixture.semester(), true);
        Enrollment autoEnrollment = enrollmentRepository.saveAndFlush(Enrollment.create(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.ENROLLED,
            EnrollmentSource.AUTO,
            null,
            LocalDateTime.now()
        ));
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiGet("/api/v1/student/enrollments")
                .cookie(accessToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.currentCredit").value(3))
            .andExpect(jsonPath("$.enrolledCourses[0].source").value("AUTO"));

        mockMvc.perform(apiDelete("/api/v1/student/enrollments/" + autoEnrollment.getId())
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue()))
            .andExpect(status().isOk());

        mockMvc.perform(apiGet("/api/v1/student/enrollments")
                .cookie(accessToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.currentCredit").value(0))
            .andExpect(jsonPath("$.summary.remainingCredit").value(18));
    }

    @Test
    void applyingOpenCourseCreatesEnrolledManualEnrollment() throws Exception {
        EnrollmentFixture fixture = saveFixture("apply-enrolled");
        saveEnrollmentSchedule(fixture.semester(), true);
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        MvcResult result = mockMvc.perform(apiPost("/api/v1/student/enrollments")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fixture.courseOffering().getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.courseOfferingId").value(fixture.courseOffering().getId()))
            .andExpect(jsonPath("$.status").value("ENROLLED"))
            .andExpect(jsonPath("$.source").value("MANUAL"))
            .andExpect(jsonPath("$.waitNo").doesNotExist())
            .andReturn();

        Long enrollmentId = ((Number) com.jayway.jsonpath.JsonPath
            .read(result.getResponse().getContentAsString(), "$.enrollmentId"))
            .longValue();
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow();
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(enrollment.getSource()).isEqualTo(EnrollmentSource.MANUAL);
    }

    @Test
    void applyingFullCourseCreatesWaitingEnrollment() throws Exception {
        EnrollmentFixture fixture = saveFixture("apply-waiting");
        saveEnrollmentSchedule(fixture.semester(), true);
        CourseOffering fullOffering = saveOffering(fixture, "ENR102-apply-waiting", "Networks", 1, DayOfWeek.TUE);
        saveEnrollment(fixture.otherStudentProfile(), fixture.semester(), fullOffering, EnrollmentStatus.ENROLLED, null);
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        MvcResult result = mockMvc.perform(apiPost("/api/v1/student/enrollments")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fullOffering.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WAITING"))
            .andExpect(jsonPath("$.waitNo").value(1))
            .andReturn();

        Long enrollmentId = ((Number) com.jayway.jsonpath.JsonPath
            .read(result.getResponse().getContentAsString(), "$.enrollmentId"))
            .longValue();
        assertThat(enrollmentRepository.findById(enrollmentId).orElseThrow().getStatus())
            .isEqualTo(EnrollmentStatus.WAITING);
    }

    @Test
    void duplicateApplyReturnsDomainError() throws Exception {
        EnrollmentFixture fixture = saveFixture("duplicate-apply");
        saveEnrollmentSchedule(fixture.semester(), true);
        saveEnrollment(fixture.studentProfile(), fixture.semester(), fixture.courseOffering(), EnrollmentStatus.ENROLLED, null);
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/enrollments")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fixture.courseOffering().getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ENROLLMENT_009"));
    }

    @Test
    void applyingOutsideEnrollmentPeriodFails() throws Exception {
        EnrollmentFixture fixture = saveFixture("closed-apply");
        saveEnrollmentSchedule(fixture.semester(), false);
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/enrollments")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fixture.courseOffering().getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ENROLLMENT_008"));
    }

    @Test
    void applyingGradeRestrictedCourseFails() throws Exception {
        EnrollmentFixture fixture = saveFixture("grade-apply");
        saveEnrollmentSchedule(fixture.semester(), true);
        saveAllowedGrade(fixture.courseOffering(), 3);
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/enrollments")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fixture.courseOffering().getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ENROLLMENT_010"));
    }

    @Test
    void applyingWithoutPrerequisiteFails() throws Exception {
        EnrollmentFixture fixture = saveFixture("prereq-apply");
        saveEnrollmentSchedule(fixture.semester(), true);
        Subject prerequisiteSubject = subjectRepository.saveAndFlush(Subject.create(
            "ENR000-prereq-apply",
            "Intro Programming",
            fixture.courseOffering().getSubject().getDepartment(),
            3,
            SubjectCategory.MAJOR_REQUIRED
        ));
        savePrerequisite(fixture.courseOffering().getSubject(), prerequisiteSubject);
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/enrollments")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fixture.courseOffering().getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ENROLLMENT_011"));
    }

    @Test
    void applyingOverCreditLimitFails() throws Exception {
        EnrollmentFixture fixture = saveFixture("credit-apply");
        saveEnrollmentSchedule(fixture.semester(), true);
        fixture.studentProfile().changeMaxCredit(3);
        CourseOffering existingOffering = saveOffering(fixture, "ENR102-credit-apply", "Databases", 30, DayOfWeek.WED);
        saveEnrollment(fixture.studentProfile(), fixture.semester(), existingOffering, EnrollmentStatus.ENROLLED, null);
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/enrollments")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fixture.courseOffering().getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ENROLLMENT_012"));
    }

    @Test
    void applyingTimeConflictingCourseFails() throws Exception {
        EnrollmentFixture fixture = saveFixture("time-apply");
        saveEnrollmentSchedule(fixture.semester(), true);
        CourseOffering existingOffering = saveOfferingWithTime(
            fixture,
            "ENR102-time-apply",
            "Operating Systems",
            30,
            DayOfWeek.MON,
            2,
            3
        );
        saveEnrollment(fixture.studentProfile(), fixture.semester(), existingOffering, EnrollmentStatus.ENROLLED, null);
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/enrollments")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fixture.courseOffering().getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ENROLLMENT_013"));
    }

    @Test
    void waitingApplyIsReturnedWithWaitNoInMyEnrollments() throws Exception {
        EnrollmentFixture fixture = saveFixture("waitno-apply");
        saveEnrollmentSchedule(fixture.semester(), true);
        CourseOffering fullOffering = saveOffering(fixture, "ENR102-waitno-apply", "AI", 1, DayOfWeek.TUE);
        saveEnrollment(fixture.otherStudentProfile(), fixture.semester(), fullOffering, EnrollmentStatus.ENROLLED, null);
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/enrollments")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fullOffering.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WAITING"));

        mockMvc.perform(apiGet("/api/v1/student/enrollments")
                .cookie(accessToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.waitingCourses[0].courseOfferingId").value(fullOffering.getId()))
            .andExpect(jsonPath("$.waitingCourses[0].waitNo").value(1));
    }

    @Test
    void studentCanGetMyEnrollmentsWithSummary() throws Exception {
        EnrollmentFixture fixture = saveFixture("summary");
        CourseOffering waitingOffering = saveOffering(fixture, "ENR102-summary", "Networks", 20, DayOfWeek.TUE);
        saveEnrollment(fixture.studentProfile(), fixture.semester(), fixture.courseOffering(), EnrollmentStatus.ENROLLED, null);
        saveEnrollment(fixture.studentProfile(), fixture.semester(), waitingOffering, EnrollmentStatus.WAITING, 1);
        saveEnrollment(fixture.otherStudentProfile(), fixture.semester(), fixture.courseOffering(), EnrollmentStatus.ENROLLED, null);
        Cookie accessToken = login(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/enrollments")
                .cookie(accessToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.semesterName").value(fixture.semester().getName()))
            .andExpect(jsonPath("$.summary.currentCredit").value(3))
            .andExpect(jsonPath("$.summary.maxCredit").value(18))
            .andExpect(jsonPath("$.summary.remainingCredit").value(15))
            .andExpect(jsonPath("$.summary.enrolledCount").value(1))
            .andExpect(jsonPath("$.summary.waitingCount").value(1))
            .andExpect(jsonPath("$.enrolledCourses[0].subjectCode").value("ENR101-summary"))
            .andExpect(jsonPath("$.waitingCourses[0].subjectCode").value("ENR102-summary"))
            .andExpect(jsonPath("$.waitingCourses[0].waitNo").value(1));
    }

    @Test
    void myEnrollmentsOnlyIncludeCurrentStudent() throws Exception {
        EnrollmentFixture fixture = saveFixture("owner");
        saveEnrollment(fixture.otherStudentProfile(), fixture.semester(), fixture.courseOffering(), EnrollmentStatus.ENROLLED, null);
        Cookie accessToken = login(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/enrollments")
                .cookie(accessToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.currentCredit").value(0))
            .andExpect(jsonPath("$.enrolledCourses").isEmpty())
            .andExpect(jsonPath("$.waitingCourses").isEmpty());
    }

    @Test
    void manualRequiredCoursesOnlyIncludeWishResultNotDone() throws Exception {
        EnrollmentFixture fixture = saveFixture("manual");
        CourseOffering doneOffering = saveOffering(fixture, "ENR102-manual", "Databases", 30, DayOfWeek.WED);
        saveWish(fixture.studentProfile(), fixture.semester(), fixture.courseOffering(), WishAutoApplyResult.NOT_SELECTED);
        saveWish(fixture.studentProfile(), fixture.semester(), doneOffering, WishAutoApplyResult.DONE);
        Cookie accessToken = login(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/enrollments/manual-required")
                .cookie(accessToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].subjectCode").value("ENR101-manual"))
            .andExpect(jsonPath("$[0].result").value("NOT_SELECTED"))
            .andExpect(jsonPath("$[0].actionAvailable").value(true))
            .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void enrollmentCourseSearchMarksMyEnrolledAndWaitingCourses() throws Exception {
        EnrollmentFixture fixture = saveFixture("search");
        CourseOffering waitingOffering = saveOffering(fixture, "ENR102-search", "Algorithms", 30, DayOfWeek.THU);
        saveEnrollment(fixture.studentProfile(), fixture.semester(), fixture.courseOffering(), EnrollmentStatus.ENROLLED, null);
        saveEnrollment(fixture.studentProfile(), fixture.semester(), waitingOffering, EnrollmentStatus.WAITING, 1);
        Cookie accessToken = login(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/enrollments/courses")
                .cookie(accessToken)
                .param("mode", "condition")
                .param("year", "2026")
                .param("term", "FIRST")
                .param("departmentName", "Computer Science"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].subjectCode").value("ENR101-search"))
            .andExpect(jsonPath("$[0].status").value("ENROLLED"))
            .andExpect(jsonPath("$[1].subjectCode").value("ENR102-search"))
            .andExpect(jsonPath("$[1].status").value("WAITING"));
    }

    @Test
    void unauthenticatedAndNonStudentRequestsAreRejected() throws Exception {
        EnrollmentFixture fixture = saveFixture("security");
        Cookie professorToken = login(fixture.professor().getLoginId());
        Cookie staffToken = login(fixture.staff().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/enrollments")
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_005"));

        mockMvc.perform(apiGet("/api/v1/student/enrollments")
                .cookie(professorToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("AUTH_006"));

        mockMvc.perform(apiGet("/api/v1/student/enrollments")
                .cookie(staffToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("AUTH_006"));
    }

    private EnrollmentFixture saveFixture(String suffix) {
        Department department = departmentRepository.saveAndFlush(
            Department.create("Computer Science", "Engineering", DepartmentType.DEPARTMENT)
        );
        Member student = saveMember("enrollment-student-" + suffix, "Student " + suffix, MemberRole.STUDENT);
        StudentProfile studentProfile = studentProfileRepository.saveAndFlush(StudentProfile.create(
            student,
            "ES-" + suffix,
            department,
            2,
            AcademicStatus.ENROLLED,
            18
        ));
        Member otherStudent = saveMember(
            "enrollment-other-student-" + suffix,
            "Other Student " + suffix,
            MemberRole.STUDENT
        );
        StudentProfile otherStudentProfile = studentProfileRepository.saveAndFlush(StudentProfile.create(
            otherStudent,
            "EOS-" + suffix,
            department,
            2,
            AcademicStatus.ENROLLED,
            18
        ));
        Member professor = saveMember(
            "enrollment-professor-" + suffix,
            "Professor " + suffix,
            MemberRole.PROFESSOR
        );
        ProfessorProfile professorProfile = professorProfileRepository.saveAndFlush(
            ProfessorProfile.create(professor, "EP-" + suffix, department, "Professor")
        );
        Member staff = saveMember("enrollment-staff-" + suffix, "Staff " + suffix, MemberRole.STAFF);
        StaffProfile staffProfile = staffProfileRepository.saveAndFlush(
            StaffProfile.create(staff, "ET-" + suffix, department, "Staff", "Courses")
        );
        Semester semester = semesterRepository.findByYearAndTerm(2026, SemesterTerm.FIRST)
            .orElseGet(() -> semesterRepository.saveAndFlush(
                Semester.create(2026, SemesterTerm.FIRST, "2026 FIRST " + suffix, 18)
            ));
        Subject subject = subjectRepository.saveAndFlush(Subject.create(
            "ENR101-" + suffix,
            "Data Structures",
            department,
            3,
            SubjectCategory.MAJOR_REQUIRED
        ));
        CourseOffering offering = courseOfferingRepository.saveAndFlush(CourseOffering.create(
            semester,
            subject,
            professorProfile,
            30,
            CourseOfferingStatus.OPEN,
            staffProfile
        ));
        courseTimeRepository.saveAndFlush(CourseTime.create(offering, DayOfWeek.MON, 1, 2));

        return new EnrollmentFixture(
            student,
            professor,
            staff,
            studentProfile,
            otherStudentProfile,
            semester,
            offering
        );
    }

    private CourseOffering saveOffering(
        EnrollmentFixture fixture,
        String subjectCode,
        String subjectName,
        int capacity,
        DayOfWeek dayOfWeek
    ) {
        return saveOfferingWithTime(
            fixture,
            subjectCode,
            subjectName,
            capacity,
            dayOfWeek,
            3,
            4
        );
    }

    private CourseOffering saveOfferingWithTime(
        EnrollmentFixture fixture,
        String subjectCode,
        String subjectName,
        int capacity,
        DayOfWeek dayOfWeek,
        int startPeriod,
        int endPeriod
    ) {
        Subject subject = subjectRepository.saveAndFlush(Subject.create(
            subjectCode,
            subjectName,
            fixture.courseOffering().getSubject().getDepartment(),
            3,
            SubjectCategory.MAJOR_REQUIRED
        ));
        CourseOffering offering = courseOfferingRepository.saveAndFlush(CourseOffering.create(
            fixture.semester(),
            subject,
            fixture.courseOffering().getProfessor(),
            capacity,
            CourseOfferingStatus.OPEN,
            fixture.courseOffering().getCreatedBy()
        ));
        courseTimeRepository.saveAndFlush(CourseTime.create(
            offering,
            dayOfWeek,
            startPeriod,
            endPeriod
        ));
        return offering;
    }

    private Enrollment saveEnrollment(
        StudentProfile student,
        Semester semester,
        CourseOffering courseOffering,
        EnrollmentStatus status,
        Integer waitNumber
    ) {
        return saveEnrollmentAt(
            student,
            semester,
            courseOffering,
            status,
            waitNumber,
            LocalDateTime.now()
        );
    }

    private Enrollment saveEnrollmentAt(
        StudentProfile student,
        Semester semester,
        CourseOffering courseOffering,
        EnrollmentStatus status,
        Integer waitNumber,
        LocalDateTime appliedAt
    ) {
        return enrollmentRepository.saveAndFlush(Enrollment.create(
            student,
            semester,
            courseOffering,
            status,
            EnrollmentSource.MANUAL,
            waitNumber,
            appliedAt
        ));
    }

    private WishCourse saveWish(
        StudentProfile student,
        Semester semester,
        CourseOffering courseOffering,
        WishAutoApplyResult result
    ) {
        return wishCourseRepository.saveAndFlush(WishCourse.create(
            student,
            semester,
            courseOffering,
            false,
            result,
            null
        ));
    }

    private WishCourse saveWishAuto(
        StudentProfile student,
        Semester semester,
        CourseOffering courseOffering,
        WishAutoApplyResult result
    ) {
        return wishCourseRepository.saveAndFlush(WishCourse.create(
            student,
            semester,
            courseOffering,
            true,
            result,
            null
        ));
    }

    private StudentProfile saveStudentProfile(
        String loginId,
        String name,
        String studentNo,
        Department department
    ) {
        Member student = saveMember(loginId, name, MemberRole.STUDENT);
        return studentProfileRepository.saveAndFlush(StudentProfile.create(
            student,
            studentNo,
            department,
            2,
            AcademicStatus.ENROLLED,
            18
        ));
    }

    private SemesterSchedule saveEnrollmentSchedule(Semester semester, boolean open) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = open ? now.minusDays(1) : now.minusDays(3);
        LocalDateTime endAt = open ? now.plusDays(1) : now.minusDays(1);
        return semesterScheduleRepository.findBySemesterIdAndType(
                semester.getId(),
                SemesterScheduleType.ENROLLMENT
            )
            .map(schedule -> {
                schedule.updatePeriod(startAt, endAt, null);
                return semesterScheduleRepository.saveAndFlush(schedule);
            })
            .orElseGet(() -> semesterScheduleRepository.saveAndFlush(SemesterSchedule.create(
                semester,
                SemesterScheduleType.ENROLLMENT,
                startAt,
                endAt,
                null,
                false
            )));
    }

    private CourseAllowedGrade saveAllowedGrade(CourseOffering courseOffering, Integer gradeLevel) {
        return courseAllowedGradeRepository.saveAndFlush(CourseAllowedGrade.create(
            courseOffering,
            gradeLevel
        ));
    }

    private CoursePrerequisite savePrerequisite(Subject subject, Subject prerequisiteSubject) {
        return coursePrerequisiteRepository.saveAndFlush(CoursePrerequisite.create(
            subject,
            prerequisiteSubject,
            true
        ));
    }

    private Member saveMember(String loginId, String name, MemberRole role) {
        return memberRepository.saveAndFlush(Member.create(
            loginId,
            passwordEncoder.encode(PASSWORD),
            name,
            role
        ));
    }

    private Cookie login(String loginId) throws Exception {
        Cookie csrfCookie = getCsrfCookie();
        MvcResult loginResult = mockMvc.perform(apiPost("/api/v1/auth/login")
                .cookie(csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .header(HttpHeaders.USER_AGENT, "MockMvc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginId":"%s","password":"%s"}
                    """.formatted(loginId, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();

        return extractResponseCookie(loginResult, ACCESS_TOKEN_COOKIE);
    }

    private Cookie getCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(apiGet("/api/v1/csrf"))
            .andExpect(status().isOk())
            .andReturn();

        return extractResponseCookie(result, CSRF_COOKIE);
    }

    private MockHttpServletRequestBuilder apiGet(String path) {
        return get(path).servletPath(path);
    }

    private MockHttpServletRequestBuilder apiPost(String path) {
        return post(path).servletPath(path);
    }

    private MockHttpServletRequestBuilder apiDelete(String path) {
        return delete(path).servletPath(path);
    }

    private Cookie extractResponseCookie(MvcResult result, String cookieName) {
        Cookie cookie = result.getResponse().getCookie(cookieName);
        if (cookie != null) {
            return cookie;
        }

        String setCookie = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
            .filter(header -> header.startsWith(cookieName + "="))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing Set-Cookie: " + cookieName));
        return new Cookie(
            cookieName,
            setCookie.substring((cookieName + "=").length(), setCookie.indexOf(';'))
        );
    }

    private record EnrollmentFixture(
        Member student,
        Member professor,
        Member staff,
        StudentProfile studentProfile,
        StudentProfile otherStudentProfile,
        Semester semester,
        CourseOffering courseOffering
    ) {
    }
}
