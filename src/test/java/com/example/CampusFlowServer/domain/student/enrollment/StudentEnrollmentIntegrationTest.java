package com.example.CampusFlowServer.domain.student.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentApplyOneResult;
import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentApplyOneStatus;
import com.example.CampusFlowServer.domain.student.enrollment.dto.AutoEnrollmentBatchLaunchResponse;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentApplyRequest;
import com.example.CampusFlowServer.domain.student.enrollment.service.AutoEnrollmentCommandService;
import com.example.CampusFlowServer.domain.student.enrollment.service.AutoEnrollmentService;
import com.example.CampusFlowServer.domain.student.enrollment.service.AutoEnrollmentBatchLaunchService;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentErrorCode;
import com.example.CampusFlowServer.domain.student.enrollment.exception.StudentEnrollmentException;
import com.example.CampusFlowServer.domain.student.enrollment.service.StudentEnrollmentApplyService;
import com.example.CampusFlowServer.domain.student.enrollment.service.StudentEnrollmentLockCommandService;
import com.example.CampusFlowServer.domain.student.enrollment.service.StudentEnrollmentService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.transaction.TestTransaction;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StudentEnrollmentIntegrationTest {

    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String PASSWORD = "password123!";

    private boolean committedFixtureTransaction;

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

    @Autowired
    private StudentEnrollmentService studentEnrollmentService;

    @Autowired
    private StudentEnrollmentApplyService studentEnrollmentApplyService;

    @Autowired
    private StudentEnrollmentLockCommandService studentEnrollmentLockCommandService;

    @Autowired
    private AutoEnrollmentCommandService autoEnrollmentCommandService;

    @Autowired
    private AutoEnrollmentService autoEnrollmentService;

    @Autowired
    private AutoEnrollmentBatchLaunchService autoEnrollmentBatchLaunchService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanCommittedEnrollmentFixtures() {
        if (!committedFixtureTransaction) {
            return;
        }
        ensureActiveTestTransaction();
        entityManager.createNativeQuery("""
            delete from wish_courses
            where course_offering_id in (
                select co.id
                from course_offerings co
                join subjects s on co.subject_id = s.id
                where s.code like 'ENR%'
            )
            or student_profile_id in (
                select sp.id
                from student_profiles sp
                join members m on sp.member_id = m.id
                where m.login_id like 'enrollment-%'
            )
            """).executeUpdate();
        entityManager.createNativeQuery("""
            delete from enrollments
            where course_offering_id in (
                select co.id
                from course_offerings co
                join subjects s on co.subject_id = s.id
                where s.code like 'ENR%'
            )
            or student_profile_id in (
                select sp.id
                from student_profiles sp
                join members m on sp.member_id = m.id
                where m.login_id like 'enrollment-%'
            )
            """).executeUpdate();
        entityManager.createNativeQuery("""
            delete from completed_courses
            where student_profile_id in (
                select sp.id
                from student_profiles sp
                join members m on sp.member_id = m.id
                where m.login_id like 'enrollment-%'
            )
            or subject_id in (select id from subjects where code like 'ENR%')
            """).executeUpdate();
        entityManager.createNativeQuery("""
            delete from course_allowed_grades
            where course_offering_id in (
                select co.id
                from course_offerings co
                join subjects s on co.subject_id = s.id
                where s.code like 'ENR%'
            )
            """).executeUpdate();
        entityManager.createNativeQuery("""
            delete from course_prerequisites
            where subject_id in (select id from subjects where code like 'ENR%')
               or prerequisite_subject_id in (select id from subjects where code like 'ENR%')
            """).executeUpdate();
        entityManager.createNativeQuery("""
            delete from course_times
            where course_offering_id in (
                select co.id
                from course_offerings co
                join subjects s on co.subject_id = s.id
                where s.code like 'ENR%'
            )
            """).executeUpdate();
        entityManager.createNativeQuery("""
            delete from course_offerings
            where subject_id in (select id from subjects where code like 'ENR%')
            """).executeUpdate();
        entityManager.createNativeQuery("delete from subjects where code like 'ENR%'")
            .executeUpdate();
        entityManager.createNativeQuery("""
            delete from refresh_tokens
            where member_id in (select id from members where login_id like 'enrollment-%')
            """).executeUpdate();
        entityManager.createNativeQuery("""
            delete from student_profiles
            where member_id in (select id from members where login_id like 'enrollment-%')
            """).executeUpdate();
        entityManager.createNativeQuery("""
            delete from professor_profiles
            where member_id in (select id from members where login_id like 'enrollment-%')
            """).executeUpdate();
        entityManager.createNativeQuery("""
            delete from staff_profiles
            where member_id in (select id from members where login_id like 'enrollment-%')
            """).executeUpdate();
        entityManager.createNativeQuery("delete from members where login_id like 'enrollment-%'")
            .executeUpdate();
        entityManager.flush();
        TestTransaction.flagForCommit();
        committedFixtureTransaction = false;
    }

    @Test
    void applyTransactionBoundariesAreSplitAcrossBeans() throws Exception {
        Class<?> studentEnrollmentServiceClass = AopUtils.getTargetClass(studentEnrollmentService);
        Class<?> applyServiceClass = AopUtils.getTargetClass(studentEnrollmentApplyService);
        Class<?> lockCommandServiceClass = AopUtils.getTargetClass(studentEnrollmentLockCommandService);
        Class<?> autoEnrollmentServiceClass = AopUtils.getTargetClass(autoEnrollmentService);
        Class<?> autoCommandServiceClass = AopUtils.getTargetClass(autoEnrollmentCommandService);

        Transactional serviceApplyTransaction = studentEnrollmentServiceClass
            .getMethod("apply", Long.class, StudentEnrollmentApplyRequest.class)
            .getAnnotation(Transactional.class);
        Transactional applyTransaction = applyServiceClass
            .getMethod("apply", Long.class, StudentEnrollmentApplyRequest.class)
            .getAnnotation(Transactional.class);
        Transactional commandTransaction = lockCommandServiceClass
            .getMethod("applyWithLock", Class.forName(
                "com.example.CampusFlowServer.domain.student.enrollment.service.EnrollmentPreValidationResult"
            ))
            .getAnnotation(Transactional.class);
        Transactional autoOrchestrationTransaction = autoEnrollmentServiceClass
            .getMethod("applyAutoEnrollments", Integer.class, SemesterTerm.class)
            .getAnnotation(Transactional.class);
        Transactional autoCommandTransaction = autoCommandServiceClass
            .getMethod("applyOne", Long.class, Long.class)
            .getAnnotation(Transactional.class);

        assertThat(applyServiceClass).isNotEqualTo(studentEnrollmentServiceClass);
        assertThat(lockCommandServiceClass).isNotEqualTo(studentEnrollmentServiceClass);
        assertThat(serviceApplyTransaction.propagation()).isEqualTo(Propagation.NOT_SUPPORTED);
        assertThat(applyTransaction.propagation()).isEqualTo(Propagation.NOT_SUPPORTED);
        assertThat(commandTransaction.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        assertThat(autoOrchestrationTransaction.propagation()).isEqualTo(
            Propagation.NOT_SUPPORTED
        );
        assertThat(autoCommandTransaction.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

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

        commitFixtureTransaction();
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

        commitFixtureTransaction();
        mockMvc.perform(apiPost("/api/v1/student/enrollments")
                .cookie(secondToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fullOffering.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.waitNo").value(1));

        commitFixtureTransaction();
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
    void waitingCourseTimeCausesTimeConflictInCourseSearch() throws Exception {
        EnrollmentFixture fixture = saveFixture("waiting-search-conflict");
        CourseOffering sameTimeOffering = saveOfferingWithTime(
            fixture,
            "ENR102-waiting-search-conflict",
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
            .andExpect(jsonPath("$[0].status").value("TIME_CONFLICT"));
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
        commitFixtureTransaction();
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
    void preApplyBatchProcessesDoneWishesAndAggregatesResults() throws Exception {
        EnrollmentFixture fixture = saveFixture("batch-main");
        saveEnrollmentSchedule(fixture.semester(), true);
        CourseOffering pendingOffering = saveOffering(
            fixture,
            "ENR102-batch-main",
            "Pending Course",
            30,
            DayOfWeek.FRI
        );
        CourseOffering activeOffering = saveOffering(
            fixture,
            "ENR103-batch-main",
            "Active Course",
            30,
            DayOfWeek.TUE
        );
        CourseOffering waitingOffering = saveOffering(
            fixture,
            "ENR106-batch-main",
            "Waiting Course",
            30,
            DayOfWeek.SAT
        );
        CourseOffering cancelledOffering = saveOffering(
            fixture,
            "ENR104-batch-main",
            "Cancelled Course",
            30,
            DayOfWeek.WED
        );
        CourseOffering fullOffering = saveOffering(
            fixture,
            "ENR105-batch-main",
            "Full Course",
            1,
            DayOfWeek.THU
        );
        saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            WishAutoApplyResult.DONE
        );
        saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            pendingOffering,
            WishAutoApplyResult.PENDING
        );
        saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            activeOffering,
            WishAutoApplyResult.DONE
        );
        saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            waitingOffering,
            WishAutoApplyResult.DONE
        );
        saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            cancelledOffering,
            WishAutoApplyResult.DONE
        );
        saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            fullOffering,
            WishAutoApplyResult.DONE
        );
        saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            activeOffering,
            EnrollmentStatus.ENROLLED,
            null
        );
        saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            waitingOffering,
            EnrollmentStatus.WAITING,
            1
        );
        Enrollment cancelled = saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            cancelledOffering,
            EnrollmentStatus.ENROLLED,
            null
        );
        cancelled.cancel(LocalDateTime.now());
        enrollmentRepository.saveAndFlush(cancelled);
        saveEnrollment(
            fixture.otherStudentProfile(),
            fixture.semester(),
            fullOffering,
            EnrollmentStatus.ENROLLED,
            null
        );
        Long studentId = fixture.studentProfile().getId();
        Long semesterId = fixture.semester().getId();
        Long cancelledEnrollmentId = cancelled.getId();
        String staffLoginId = fixture.staff().getLoginId();
        commitFixtureTransaction();
        Cookie staffToken = login(staffLoginId);
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/staff/auto-enrollments/pre-apply-batch")
                .cookie(staffToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobName").value("autoEnrollmentPreApplyJob"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.semesterId").value(semesterId))
            .andExpect(jsonPath("$.targetCount").value(5))
            .andExpect(jsonPath("$.appliedCount").value(2))
            .andExpect(jsonPath("$.skippedCount").value(3))
            .andExpect(jsonPath("$.failedCount").value(0))
            .andExpect(jsonPath("$.statusCounts.APPLIED").value(1))
            .andExpect(jsonPath("$.statusCounts.REACTIVATED").value(1))
            .andExpect(jsonPath("$.statusCounts.SKIPPED_ALREADY_ACTIVE").value(2))
            .andExpect(jsonPath("$.statusCounts.SKIPPED_OVER_CAPACITY").value(1))
            .andExpect(jsonPath("$.enrollmentStartAt").exists())
            .andExpect(jsonPath("$.businessDate").exists());

        List<Enrollment> activeEnrollments = enrollmentRepository
            .findByStudentIdAndSemesterIdAndStatusIn(
                studentId,
                semesterId,
                List.of(EnrollmentStatus.ENROLLED, EnrollmentStatus.WAITING)
            );
        assertThat(activeEnrollments).hasSize(4);
        assertThat(activeEnrollments)
            .filteredOn(enrollment -> EnrollmentSource.AUTO.equals(enrollment.getSource()))
            .hasSize(2);
        assertThat(enrollmentRepository.findById(cancelledEnrollmentId).orElseThrow().getStatus())
            .isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(enrollmentRepository
            .findFirstByStudentIdAndCourseOfferingIdOrderByIdDesc(
                studentId,
                fullOffering.getId()
            ))
            .isEmpty();
    }

    @Test
    void completedPreApplyBatchRejectsSameJobParametersWithoutCreatingDuplicate() {
        EnrollmentFixture fixture = saveFixture("batch-repeat");
        saveEnrollmentSchedule(fixture.semester(), true);
        saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            WishAutoApplyResult.DONE
        );
        Long studentId = fixture.studentProfile().getId();
        Long semesterId = fixture.semester().getId();
        commitFixtureTransaction();

        AutoEnrollmentBatchLaunchResponse first = autoEnrollmentBatchLaunchService.launch(
            2026,
            SemesterTerm.FIRST
        );

        assertThat(first.status()).isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from BATCH_JOB_INSTANCE where JOB_NAME = ?",
            Long.class,
            "autoEnrollmentPreApplyJob"
        )).isPositive();
        assertThatThrownBy(() -> autoEnrollmentBatchLaunchService.launch(
            2026,
            SemesterTerm.FIRST
        ))
            .isInstanceOf(StudentEnrollmentException.class)
            .extracting(exception -> ((StudentEnrollmentException) exception).getErrorCode())
            .isEqualTo(StudentEnrollmentErrorCode.AUTO_ENROLLMENT_BATCH_ALREADY_COMPLETED);
        assertThat(enrollmentRepository.findByStudentIdAndSemesterIdAndStatusIn(
            studentId,
            semesterId,
            List.of(EnrollmentStatus.ENROLLED, EnrollmentStatus.WAITING)
        )).hasSize(1);
    }

    @Test
    void preApplyBatchExcludesAllNonDoneWishResults() {
        EnrollmentFixture fixture = saveFixture("batch-exclude");
        saveEnrollmentSchedule(fixture.semester(), true);
        List<WishAutoApplyResult> excludedResults = List.of(
            WishAutoApplyResult.PENDING,
            WishAutoApplyResult.OVER_CAPACITY,
            WishAutoApplyResult.NEEDS_MANUAL,
            WishAutoApplyResult.TIME_CONFLICT
        );
        List<CourseOffering> offerings = List.of(
            fixture.courseOffering(),
            saveOffering(fixture, "ENR102-batch-exclude", "Excluded 2", 30, DayOfWeek.TUE),
            saveOffering(fixture, "ENR103-batch-exclude", "Excluded 3", 30, DayOfWeek.WED),
            saveOffering(fixture, "ENR104-batch-exclude", "Excluded 4", 30, DayOfWeek.THU)
        );
        for (int i = 0; i < excludedResults.size(); i++) {
            saveWishAuto(
                fixture.studentProfile(),
                fixture.semester(),
                offerings.get(i),
                excludedResults.get(i)
            );
        }
        Long studentId = fixture.studentProfile().getId();
        Long semesterId = fixture.semester().getId();
        commitFixtureTransaction();

        AutoEnrollmentBatchLaunchResponse response = autoEnrollmentBatchLaunchService.launch(
            2026,
            SemesterTerm.FIRST
        );

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.targetCount()).isZero();
        assertThat(response.appliedCount()).isZero();
        assertThat(response.skippedCount()).isZero();
        assertThat(enrollmentRepository.findByStudentIdAndSemesterIdAndStatusIn(
            studentId,
            semesterId,
            List.of(EnrollmentStatus.ENROLLED, EnrollmentStatus.WAITING)
        )).isEmpty();
    }

    @Test
    void autoEnrollmentCommandCreatesEnrolledAutoEnrollment() {
        EnrollmentFixture fixture = saveFixture("ac-create");
        WishCourse wishCourse = saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            WishAutoApplyResult.DONE
        );

        AutoEnrollmentApplyOneResult result = applyAutoCommand(wishCourse);

        Enrollment enrollment = enrollmentRepository.findById(result.enrollmentId()).orElseThrow();
        assertThat(result.status()).isEqualTo(AutoEnrollmentApplyOneStatus.APPLIED);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(enrollment.getSource()).isEqualTo(EnrollmentSource.AUTO);
    }

    @Test
    void autoEnrollmentCommandSkipsExistingEnrolledEnrollment() {
        assertAutoCommandSkipsActiveEnrollment(EnrollmentStatus.ENROLLED, null);
    }

    @Test
    void autoEnrollmentCommandSkipsExistingWaitingEnrollment() {
        assertAutoCommandSkipsActiveEnrollment(EnrollmentStatus.WAITING, 1);
    }

    @Test
    void autoEnrollmentCommandReactivatesCancelledEnrollment() {
        EnrollmentFixture fixture = saveFixture("ac-react");
        WishCourse wishCourse = saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            WishAutoApplyResult.DONE
        );
        Enrollment cancelled = saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.ENROLLED,
            null
        );
        cancelled.cancel(LocalDateTime.now());
        enrollmentRepository.saveAndFlush(cancelled);
        Long cancelledId = cancelled.getId();

        AutoEnrollmentApplyOneResult result = applyAutoCommand(wishCourse);

        Enrollment reactivated = enrollmentRepository.findById(cancelledId).orElseThrow();
        assertThat(result.status()).isEqualTo(AutoEnrollmentApplyOneStatus.REACTIVATED);
        assertThat(result.enrollmentId()).isEqualTo(cancelledId);
        assertThat(reactivated.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(reactivated.getSource()).isEqualTo(EnrollmentSource.AUTO);
        assertThat(reactivated.getCancelledAt()).isNull();
    }

    @Test
    void autoEnrollmentCommandSkipsWhenCourseIsFullWithoutCreatingWaitingEnrollment() {
        EnrollmentFixture fixture = saveFixture("ac-full");
        CourseOffering fullOffering = saveOffering(
            fixture,
            "ENR102-ac-full",
            "Full Course",
            1,
            DayOfWeek.TUE
        );
        WishCourse wishCourse = saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            fullOffering,
            WishAutoApplyResult.DONE
        );
        saveEnrollment(
            fixture.otherStudentProfile(),
            fixture.semester(),
            fullOffering,
            EnrollmentStatus.ENROLLED,
            null
        );
        Long studentId = fixture.studentProfile().getId();
        Long offeringId = fullOffering.getId();

        AutoEnrollmentApplyOneResult result = applyAutoCommand(wishCourse);

        assertThat(result.status()).isEqualTo(
            AutoEnrollmentApplyOneStatus.SKIPPED_OVER_CAPACITY
        );
        assertThat(enrollmentRepository
            .findFirstByStudentIdAndCourseOfferingIdOrderByIdDesc(studentId, offeringId))
            .isEmpty();
    }

    @Test
    void autoEnrollmentCommandSkipsCreditLimitViolation() {
        EnrollmentFixture fixture = saveFixture("ac-credit");
        fixture.studentProfile().changeMaxCredit(2);
        studentProfileRepository.saveAndFlush(fixture.studentProfile());
        WishCourse wishCourse = saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            WishAutoApplyResult.DONE
        );

        AutoEnrollmentApplyOneResult result = applyAutoCommand(wishCourse);

        assertThat(result.status()).isEqualTo(AutoEnrollmentApplyOneStatus.SKIPPED_CREDIT_LIMIT);
        assertThat(result.enrollmentId()).isNull();
    }

    @Test
    void autoEnrollmentCommandSkipsTimeConflict() {
        EnrollmentFixture fixture = saveFixture("ac-time");
        CourseOffering existingOffering = saveOfferingWithTime(
            fixture,
            "ENR102-ac-time",
            "Conflicting Course",
            30,
            DayOfWeek.MON,
            1,
            2
        );
        saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            existingOffering,
            EnrollmentStatus.ENROLLED,
            null
        );
        WishCourse wishCourse = saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            WishAutoApplyResult.DONE
        );

        AutoEnrollmentApplyOneResult result = applyAutoCommand(wishCourse);

        assertThat(result.status()).isEqualTo(AutoEnrollmentApplyOneStatus.SKIPPED_TIME_CONFLICT);
        assertThat(result.enrollmentId()).isNull();
    }

    @Test
    void autoEnrollmentCommandSkipsGradeRestriction() {
        EnrollmentFixture fixture = saveFixture("ac-grade");
        saveAllowedGrade(fixture.courseOffering(), 3);
        WishCourse wishCourse = saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            WishAutoApplyResult.DONE
        );

        AutoEnrollmentApplyOneResult result = applyAutoCommand(wishCourse);

        assertThat(result.status()).isEqualTo(
            AutoEnrollmentApplyOneStatus.SKIPPED_GRADE_RESTRICTED
        );
        assertThat(result.enrollmentId()).isNull();
    }

    @Test
    void autoEnrollmentCommandSkipsMissingPrerequisite() {
        EnrollmentFixture fixture = saveFixture("ac-prereq");
        Subject prerequisite = subjectRepository.saveAndFlush(Subject.create(
            "ENR-PRE-ac-prereq",
            "Prerequisite",
            fixture.courseOffering().getSubject().getDepartment(),
            3,
            SubjectCategory.MAJOR_REQUIRED
        ));
        savePrerequisite(fixture.courseOffering().getSubject(), prerequisite);
        WishCourse wishCourse = saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            WishAutoApplyResult.DONE
        );

        AutoEnrollmentApplyOneResult result = applyAutoCommand(wishCourse);

        assertThat(result.status()).isEqualTo(
            AutoEnrollmentApplyOneStatus.SKIPPED_PREREQUISITE_REQUIRED
        );
        assertThat(result.enrollmentId()).isNull();
    }

    @ParameterizedTest
    @EnumSource(
        value = WishAutoApplyResult.class,
        names = {"PENDING", "OVER_CAPACITY", "NEEDS_MANUAL", "TIME_CONFLICT"}
    )
    void autoEnrollmentCommandSkipsWishCourseThatIsNotDone(WishAutoApplyResult resultStatus) {
        EnrollmentFixture fixture = saveFixture("ac-nd-" + resultStatus.name());
        WishCourse wishCourse = saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            resultStatus
        );

        AutoEnrollmentApplyOneResult result = applyAutoCommand(wishCourse);

        assertThat(result.status()).isEqualTo(AutoEnrollmentApplyOneStatus.SKIPPED_NOT_DONE);
        assertThat(result.enrollmentId()).isNull();
    }

    @Test
    void autoEnrollmentCommandIsIdempotentWhenRunAgain() {
        EnrollmentFixture fixture = saveFixture("ac-idem");
        WishCourse wishCourse = saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            WishAutoApplyResult.DONE
        );
        Long wishCourseId = wishCourse.getId();
        Long offeringId = fixture.courseOffering().getId();
        Long studentId = fixture.studentProfile().getId();
        commitFixtureTransaction();

        AutoEnrollmentApplyOneResult first = autoEnrollmentCommandService.applyOne(
            wishCourseId,
            offeringId
        );
        AutoEnrollmentApplyOneResult second = autoEnrollmentCommandService.applyOne(
            wishCourseId,
            offeringId
        );

        assertThat(first.status()).isEqualTo(AutoEnrollmentApplyOneStatus.APPLIED);
        assertThat(second.status()).isEqualTo(AutoEnrollmentApplyOneStatus.SKIPPED_ALREADY_ACTIVE);
        assertThat(enrollmentRepository.findByStudentIdAndSemesterIdAndStatusIn(
            studentId,
            fixture.semester().getId(),
            List.of(EnrollmentStatus.ENROLLED, EnrollmentStatus.WAITING)
        )).hasSize(1);
    }

    @Test
    void concurrentAutoEnrollmentCommandsDoNotExceedCourseCapacity() throws Exception {
        EnrollmentFixture fixture = saveFixture("ac-lock");
        CourseOffering oneSeatOffering = saveOffering(
            fixture,
            "ENR102-ac-lock",
            "One Seat Course",
            1,
            DayOfWeek.TUE
        );
        WishCourse firstWish = saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            oneSeatOffering,
            WishAutoApplyResult.DONE
        );
        WishCourse secondWish = saveWishAuto(
            fixture.otherStudentProfile(),
            fixture.semester(),
            oneSeatOffering,
            WishAutoApplyResult.DONE
        );
        Long offeringId = oneSeatOffering.getId();
        Long firstWishId = firstWish.getId();
        Long secondWishId = secondWish.getId();
        commitFixtureTransaction();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<AutoEnrollmentApplyOneResult> first = executor.submit(() -> {
                start.await();
                return autoEnrollmentCommandService.applyOne(firstWishId, offeringId);
            });
            Future<AutoEnrollmentApplyOneResult> second = executor.submit(() -> {
                start.await();
                return autoEnrollmentCommandService.applyOne(secondWishId, offeringId);
            });
            start.countDown();

            List<AutoEnrollmentApplyOneStatus> statuses = List.of(
                first.get().status(),
                second.get().status()
            );
            assertThat(statuses).containsExactlyInAnyOrder(
                AutoEnrollmentApplyOneStatus.APPLIED,
                AutoEnrollmentApplyOneStatus.SKIPPED_OVER_CAPACITY
            );
        } finally {
            executor.shutdownNow();
        }

        long enrolledCount = enrollmentRepository.countByCourseOfferingIdsAndStatuses(
                List.of(offeringId),
                List.of(EnrollmentStatus.ENROLLED)
            )
            .stream()
            .mapToLong(projection -> projection.getCount())
            .sum();
        assertThat(enrolledCount).isEqualTo(1);
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
            .andExpect(jsonPath("$.summary.activeCredit").value(3))
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
            .andExpect(jsonPath("$.summary.activeCredit").value(0))
            .andExpect(jsonPath("$.summary.remainingCredit").value(18));
    }

    @Test
    void applyingOpenCourseCreatesEnrolledManualEnrollment() throws Exception {
        EnrollmentFixture fixture = saveFixture("apply-enrolled");
        saveEnrollmentSchedule(fixture.semester(), true);
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        commitFixtureTransaction();
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

        commitFixtureTransaction();
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
    void applyingFullCourseReturnsSequentialWaitNoByCount() throws Exception {
        EnrollmentFixture fixture = saveFixture("apply-waiting-sequence");
        saveEnrollmentSchedule(fixture.semester(), true);
        CourseOffering fullOffering = saveOffering(
            fixture,
            "ENR102-apply-waiting-sequence",
            "Networks",
            1,
            DayOfWeek.TUE
        );
        StudentProfile thirdStudentProfile = saveStudentProfile(
            "enrollment-third-student-apply-waiting-sequence",
            "Third Student apply-waiting-sequence",
            "ETS-apply-waiting-sequence",
            fixture.courseOffering().getSubject().getDepartment()
        );
        saveEnrollment(
            fixture.otherStudentProfile(),
            fixture.semester(),
            fullOffering,
            EnrollmentStatus.ENROLLED,
            null
        );

        applyEnrollment(fixture.student(), fullOffering)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WAITING"))
            .andExpect(jsonPath("$.waitNo").value(1));

        applyEnrollment(thirdStudentProfile.getMember(), fullOffering)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WAITING"))
            .andExpect(jsonPath("$.waitNo").value(2));
    }

    @Test
    void reapplyingCancelledEnrollmentUsesUpdatedAppliedAtForWaitNo() throws Exception {
        EnrollmentFixture fixture = saveFixture("reapply-wait-count");
        saveEnrollmentSchedule(fixture.semester(), true);
        CourseOffering fullOffering = saveOffering(
            fixture,
            "ENR102-reapply-wait-count",
            "Networks",
            1,
            DayOfWeek.TUE
        );
        saveEnrollment(
            fixture.otherStudentProfile(),
            fixture.semester(),
            fullOffering,
            EnrollmentStatus.ENROLLED,
            null
        );
        saveEnrollmentAt(
            saveStudentProfile(
                "enrollment-third-student-reapply-wait-count",
                "Third Student reapply-wait-count",
                "ETS-reapply-wait-count",
                fixture.courseOffering().getSubject().getDepartment()
            ),
            fixture.semester(),
            fullOffering,
            EnrollmentStatus.WAITING,
            null,
            LocalDateTime.now().minusMinutes(1)
        );
        Enrollment cancelled = saveEnrollmentAt(
            fixture.studentProfile(),
            fixture.semester(),
            fullOffering,
            EnrollmentStatus.WAITING,
            null,
            LocalDateTime.now().minusMinutes(10)
        );
        cancelled.cancel(LocalDateTime.now().minusMinutes(5));
        enrollmentRepository.saveAndFlush(cancelled);

        applyEnrollment(fixture.student(), fullOffering)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WAITING"))
            .andExpect(jsonPath("$.waitNo").value(2));
    }

    @Test
    void countWaitingPositionUsesAppliedAtIdAndFiltersStatusAndCourseOffering() {
        EnrollmentFixture fixture = saveFixture("wait-count-query");
        CourseOffering otherOffering = saveOffering(
            fixture,
            "ENR102-wait-count-query",
            "Networks",
            1,
            DayOfWeek.TUE
        );
        LocalDateTime baseTime = LocalDateTime.now();
        Enrollment earlier = saveEnrollmentAt(
            fixture.otherStudentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.WAITING,
            null,
            baseTime.minusMinutes(1)
        );
        Enrollment sameTimeFirst = saveEnrollmentAt(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.WAITING,
            null,
            baseTime
        );
        StudentProfile thirdStudentProfile = saveStudentProfile(
            "enrollment-third-student-wait-count-query",
            "Third Student wait-count-query",
            "ETS-wait-count-query",
            fixture.courseOffering().getSubject().getDepartment()
        );
        Enrollment sameTimeSecond = saveEnrollmentAt(
            thirdStudentProfile,
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.WAITING,
            null,
            baseTime
        );
        StudentProfile fourthStudentProfile = saveStudentProfile(
            "enrollment-fourth-student-wait-count-query",
            "Fourth Student wait-count-query",
            "EFS-wait-count-query",
            fixture.courseOffering().getSubject().getDepartment()
        );
        Enrollment cancelled = saveEnrollmentAt(
            fourthStudentProfile,
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.WAITING,
            null,
            baseTime.minusMinutes(2)
        );
        cancelled.cancel(LocalDateTime.now());
        enrollmentRepository.saveAndFlush(cancelled);
        saveEnrollmentAt(
            saveStudentProfile(
                "enrollment-fifth-student-wait-count-query",
                "Fifth Student wait-count-query",
                "EFTS-wait-count-query",
                fixture.courseOffering().getSubject().getDepartment()
            ),
            fixture.semester(),
            otherOffering,
            EnrollmentStatus.WAITING,
            null,
            baseTime.minusMinutes(2)
        );

        assertThat(enrollmentRepository.countWaitingPosition(
            fixture.courseOffering().getId(),
            EnrollmentStatus.WAITING,
            earlier.getId()
        )).isEqualTo(1);
        assertThat(enrollmentRepository.countWaitingPosition(
            fixture.courseOffering().getId(),
            EnrollmentStatus.WAITING,
            sameTimeFirst.getId()
        )).isEqualTo(2);
        assertThat(enrollmentRepository.countWaitingPosition(
            fixture.courseOffering().getId(),
            EnrollmentStatus.WAITING,
            sameTimeSecond.getId()
        )).isEqualTo(3);
    }

    @Test
    void duplicateApplyReturnsDomainError() throws Exception {
        EnrollmentFixture fixture = saveFixture("duplicate-apply");
        saveEnrollmentSchedule(fixture.semester(), true);
        saveEnrollment(fixture.studentProfile(), fixture.semester(), fixture.courseOffering(), EnrollmentStatus.ENROLLED, null);
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        commitFixtureTransaction();
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

        commitFixtureTransaction();
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

        commitFixtureTransaction();
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

        commitFixtureTransaction();
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

        commitFixtureTransaction();
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
    void applyingCourseFailsWhenWaitingCreditExceedsLimit() throws Exception {
        EnrollmentFixture fixture = saveFixture("waiting-credit-apply");
        saveEnrollmentSchedule(fixture.semester(), true);
        fixture.studentProfile().changeMaxCredit(3);
        CourseOffering waitingOffering = saveOffering(
            fixture,
            "ENR102-waiting-credit-apply",
            "Databases",
            1,
            DayOfWeek.WED
        );
        saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            waitingOffering,
            EnrollmentStatus.WAITING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        commitFixtureTransaction();
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

        commitFixtureTransaction();
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
    void applyingCourseFailsWhenWaitingCourseTimeConflicts() throws Exception {
        EnrollmentFixture fixture = saveFixture("waiting-time-apply");
        saveEnrollmentSchedule(fixture.semester(), true);
        CourseOffering waitingOffering = saveOfferingWithTime(
            fixture,
            "ENR102-waiting-time-apply",
            "Operating Systems",
            1,
            DayOfWeek.MON,
            2,
            3
        );
        saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            waitingOffering,
            EnrollmentStatus.WAITING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        commitFixtureTransaction();
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
    void cancellingWaitingEnrollmentReleasesTimeConflictConstraint() throws Exception {
        EnrollmentFixture fixture = saveFixture("waiting-cancel-release");
        saveEnrollmentSchedule(fixture.semester(), true);
        CourseOffering conflictingOffering = saveOfferingWithTime(
            fixture,
            "ENR102-waiting-cancel-release",
            "Operating Systems",
            30,
            DayOfWeek.MON,
            2,
            3
        );
        Enrollment waiting = saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            EnrollmentStatus.WAITING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        commitFixtureTransaction();
        mockMvc.perform(apiPost("/api/v1/student/enrollments")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(conflictingOffering.getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ENROLLMENT_013"));

        mockMvc.perform(apiDelete("/api/v1/student/enrollments/" + waiting.getId())
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        commitFixtureTransaction();
        mockMvc.perform(apiPost("/api/v1/student/enrollments")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(conflictingOffering.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ENROLLED"));
    }

    @Test
    void promotingWaitingEnrollmentExcludesItselfFromActiveValidation() throws Exception {
        EnrollmentFixture fixture = saveFixture("promote-exclude-self");
        saveEnrollmentSchedule(fixture.semester(), true);
        fixture.otherStudentProfile().changeMaxCredit(3);
        CourseOffering fullOffering = saveOffering(
            fixture,
            "ENR102-promote-exclude-self",
            "Networks",
            1,
            DayOfWeek.TUE
        );
        Enrollment enrolled = saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            fullOffering,
            EnrollmentStatus.ENROLLED,
            null
        );
        Enrollment waiting = saveEnrollment(
            fixture.otherStudentProfile(),
            fixture.semester(),
            fullOffering,
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
    void waitingApplyIsReturnedWithWaitNoInMyEnrollments() throws Exception {
        EnrollmentFixture fixture = saveFixture("waitno-apply");
        saveEnrollmentSchedule(fixture.semester(), true);
        CourseOffering fullOffering = saveOffering(fixture, "ENR102-waitno-apply", "AI", 1, DayOfWeek.TUE);
        saveEnrollment(fixture.otherStudentProfile(), fixture.semester(), fullOffering, EnrollmentStatus.ENROLLED, null);
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        commitFixtureTransaction();
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
            .andExpect(jsonPath("$.summary.activeCredit").value(6))
            .andExpect(jsonPath("$.summary.maxCredit").value(18))
            .andExpect(jsonPath("$.summary.remainingCredit").value(12))
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
            .andExpect(jsonPath("$.summary.activeCredit").value(0))
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

        MvcResult result = mockMvc.perform(apiGet("/api/v1/student/enrollments/courses")
                .cookie(accessToken)
                .param("mode", "condition")
                .param("year", "2026")
                .param("term", "FIRST")
                .param("departmentName", "Computer Science"))
            .andExpect(status().isOk())
            .andReturn();

        List<String> enrolledStatuses = com.jayway.jsonpath.JsonPath.read(
            result.getResponse().getContentAsString(),
            "$[?(@.subjectCode == 'ENR101-search')].status"
        );
        List<String> waitingStatuses = com.jayway.jsonpath.JsonPath.read(
            result.getResponse().getContentAsString(),
            "$[?(@.subjectCode == 'ENR102-search')].status"
        );
        assertThat(enrolledStatuses).containsExactly("ENROLLED");
        assertThat(waitingStatuses).containsExactly("WAITING");
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

    private AutoEnrollmentApplyOneResult applyAutoCommand(WishCourse wishCourse) {
        Long wishCourseId = wishCourse.getId();
        Long offeringId = wishCourse.getCourseOffering().getId();
        commitFixtureTransaction();
        return autoEnrollmentCommandService.applyOne(wishCourseId, offeringId);
    }

    private void assertAutoCommandSkipsActiveEnrollment(
        EnrollmentStatus status,
        Integer waitNumber
    ) {
        EnrollmentFixture fixture = saveFixture("ac-act-" + status.name());
        WishCourse wishCourse = saveWishAuto(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            WishAutoApplyResult.DONE
        );
        Enrollment existing = saveEnrollment(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            status,
            waitNumber
        );
        Long existingId = existing.getId();

        AutoEnrollmentApplyOneResult result = applyAutoCommand(wishCourse);

        Enrollment unchanged = enrollmentRepository.findById(existingId).orElseThrow();
        assertThat(result.status()).isEqualTo(
            AutoEnrollmentApplyOneStatus.SKIPPED_ALREADY_ACTIVE
        );
        assertThat(unchanged.getStatus()).isEqualTo(status);
        assertThat(unchanged.getSource()).isEqualTo(EnrollmentSource.MANUAL);
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

    private ResultActions applyEnrollment(Member student, CourseOffering courseOffering) throws Exception {
        commitFixtureTransaction();
        Cookie accessToken = login(student.getLoginId());
        Cookie csrfCookie = getCsrfCookie();
        return mockMvc.perform(apiPost("/api/v1/student/enrollments")
            .cookie(accessToken, csrfCookie)
            .header(CSRF_HEADER, csrfCookie.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"courseOfferingId":%d}
                """.formatted(courseOffering.getId())));
    }

    private void commitFixtureTransaction() {
        if (TestTransaction.isActive()) {
            TestTransaction.flagForCommit();
            TestTransaction.end();
            committedFixtureTransaction = true;
        }
    }

    private void ensureActiveTestTransaction() {
        if (!TestTransaction.isActive()) {
            TestTransaction.start();
        }
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
