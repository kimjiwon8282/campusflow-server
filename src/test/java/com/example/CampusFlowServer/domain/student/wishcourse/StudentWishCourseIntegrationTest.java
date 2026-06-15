package com.example.CampusFlowServer.domain.student.wishcourse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.example.CampusFlowServer.domain.enrollment.entity.CompletedCourse;
import com.example.CampusFlowServer.domain.enrollment.entity.CourseAllowedGrade;
import com.example.CampusFlowServer.domain.enrollment.entity.CoursePrerequisite;
import com.example.CampusFlowServer.domain.enrollment.entity.WishCourse;
import com.example.CampusFlowServer.domain.enrollment.enums.WishAutoApplyResult;
import com.example.CampusFlowServer.domain.enrollment.repository.CompletedCourseRepository;
import com.example.CampusFlowServer.domain.enrollment.repository.CourseAllowedGradeRepository;
import com.example.CampusFlowServer.domain.enrollment.repository.CoursePrerequisiteRepository;
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
import com.example.CampusFlowServer.domain.semester.repository.SemesterScheduleRepository;
import com.example.CampusFlowServer.domain.semester.repository.SemesterRepository;
import jakarta.persistence.EntityManager;
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
class StudentWishCourseIntegrationTest {

    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String PASSWORD = "password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

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
    private WishCourseRepository wishCourseRepository;

    @Autowired
    private CourseAllowedGradeRepository courseAllowedGradeRepository;

    @Autowired
    private CoursePrerequisiteRepository coursePrerequisiteRepository;

    @Autowired
    private CompletedCourseRepository completedCourseRepository;

    @Test
    void studentCanCreateWishCourse() throws Exception {
        WishFixture fixture = saveWishFixture("create");
        saveWishlistSchedule(fixture.semester(), true);
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fixture.courseOffering().getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.courseOfferingId").value(fixture.courseOffering().getId()))
            .andExpect(jsonPath("$.autoApply").value(false))
            .andExpect(jsonPath("$.result").value("NOT_SELECTED"));
    }

    @Test
    void duplicateWishCourseCreateReturnsDomainError() throws Exception {
        WishFixture fixture = saveWishFixture("duplicate-create");
        saveWishlistSchedule(fixture.semester(), true);
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            false,
            WishAutoApplyResult.NOT_SELECTED,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fixture.courseOffering().getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("WISH_008"))
            .andExpect(jsonPath("$.message").value("이미 희망과목에 담긴 강의입니다."));
    }

    @Test
    void createWishCourseWithUnknownCourseOfferingReturnsDomainError() throws Exception {
        WishFixture fixture = saveWishFixture("unknown-offering");
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":999999}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("WISH_006"));
    }

    @Test
    void createWishCourseOutsideWishlistPeriodReturnsDomainError() throws Exception {
        WishFixture fixture = saveWishFixture("closed-period");
        saveWishlistSchedule(fixture.semester(), false);
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fixture.courseOffering().getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("WISH_007"))
            .andExpect(jsonPath("$.message").value("희망과목 담기 기간이 아닙니다."));
    }

    @Test
    void studentCanDeleteOwnWishCourse() throws Exception {
        WishFixture fixture = saveWishFixture("delete");
        saveWishlistSchedule(fixture.semester(), true);
        WishCourse wishCourse = saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            false,
            WishAutoApplyResult.NOT_SELECTED,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiDelete("/api/v1/student/wish-courses/" + wishCourse.getId())
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue()))
            .andExpect(status().isNoContent());

        assertThat(wishCourseRepository.findById(wishCourse.getId())).isEmpty();
    }

    @Test
    void deletingOthersWishCourseReturnsDomainError() throws Exception {
        WishFixture fixture = saveWishFixture("delete-owner");
        saveWishlistSchedule(fixture.semester(), true);
        WishCourse wishCourse = saveWishCourse(
            fixture.otherStudentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            false,
            WishAutoApplyResult.NOT_SELECTED,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiDelete("/api/v1/student/wish-courses/" + wishCourse.getId())
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("WISH_010"));
    }

    @Test
    void studentCanEnableAndDisableAutoApply() throws Exception {
        WishFixture fixture = saveWishFixture("auto-apply");
        saveWishlistSchedule(fixture.semester(), true);
        WishCourse wishCourse = saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            false,
            WishAutoApplyResult.NOT_SELECTED,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPatch("/api/v1/student/wish-courses/" + wishCourse.getId() + "/auto-apply")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"autoApply":true}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.autoApply").value(true))
            .andExpect(jsonPath("$.result").value("PENDING"));

        mockMvc.perform(apiPatch("/api/v1/student/wish-courses/" + wishCourse.getId() + "/auto-apply")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"autoApply":false}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.autoApply").value(false))
            .andExpect(jsonPath("$.result").value("NOT_SELECTED"));
    }

    @Test
    void updatingOthersAutoApplyReturnsDomainError() throws Exception {
        WishFixture fixture = saveWishFixture("auto-owner");
        saveWishlistSchedule(fixture.semester(), true);
        WishCourse wishCourse = saveWishCourse(
            fixture.otherStudentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            false,
            WishAutoApplyResult.NOT_SELECTED,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPatch("/api/v1/student/wish-courses/" + wishCourse.getId() + "/auto-apply")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"autoApply":true}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("WISH_010"));
    }

    @Test
    void professorCannotCreateWishCourse() throws Exception {
        WishFixture fixture = saveWishFixture("professor-create");
        saveWishlistSchedule(fixture.semester(), true);
        Cookie accessToken = login(fixture.professor().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fixture.courseOffering().getId())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("AUTH_006"));
    }

    @Test
    void unauthenticatedCreateWishCourseReturnsUnauthorized() throws Exception {
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses")
                .cookie(csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":1}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_005"));
    }

    @Test
    void writeApisDoNotCreateEnrollmentOrChangeCapacity() throws Exception {
        WishFixture fixture = saveWishFixture("no-enrollment-write");
        saveWishlistSchedule(fixture.semester(), true);
        Integer originalCapacity = fixture.courseOffering().getCapacity();
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        MvcResult createResult = mockMvc.perform(apiPost("/api/v1/student/wish-courses")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"courseOfferingId":%d}
                    """.formatted(fixture.courseOffering().getId())))
            .andExpect(status().isOk())
            .andReturn();
        Long wishCourseId = ((Number) com.jayway.jsonpath.JsonPath
            .read(createResult.getResponse().getContentAsString(), "$.wishCourseId"))
            .longValue();

        mockMvc.perform(apiPatch("/api/v1/student/wish-courses/" + wishCourseId + "/auto-apply")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"autoApply":true}
                    """))
            .andExpect(status().isOk());
        mockMvc.perform(apiDelete("/api/v1/student/wish-courses/" + wishCourseId)
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue()))
            .andExpect(status().isNoContent());

        Number enrollmentCount = (Number) entityManager
            .createNativeQuery("select count(*) from enrollments")
            .getSingleResult();
        assertThat(enrollmentCount.longValue()).isZero();
        assertThat(courseOfferingRepository.findById(fixture.courseOffering().getId()).orElseThrow()
            .getCapacity()).isEqualTo(originalCapacity);
    }

    @Test
    void wishCourseCreateDefaultsNullResultToNotSelected() {
        WishFixture fixture = saveWishFixture("null-result");

        WishCourse wishCourse = wishCourseRepository.saveAndFlush(WishCourse.create(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            false,
            null,
            null
        ));

        assertThat(wishCourse.getResult()).isEqualTo(WishAutoApplyResult.NOT_SELECTED);
    }

    @Test
    void wishCourseCreateDefaultsNullResultToPendingWhenAutoApplyIsTrue() {
        WishFixture fixture = saveWishFixture("null-result-pending");

        WishCourse wishCourse = wishCourseRepository.saveAndFlush(WishCourse.create(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            true,
            null,
            null
        ));

        assertThat(wishCourse.getResult()).isEqualTo(WishAutoApplyResult.PENDING);
    }

    @Test
    void enableAutoApplyClearsPreviousResultMessage() {
        WishFixture fixture = saveWishFixture("clear-message");
        WishCourse wishCourse = saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            true,
            WishAutoApplyResult.OVER_CAPACITY,
            "이전 메시지"
        );

        wishCourse.enableAutoApply();

        assertThat(wishCourse.getResult()).isEqualTo(WishAutoApplyResult.PENDING);
        assertThat(wishCourse.getResultMessage()).isNull();
    }

    @Test
    void missingAutoApplyValueReturnsDomainError() throws Exception {
        WishFixture fixture = saveWishFixture("missing-auto-apply");
        saveWishlistSchedule(fixture.semester(), true);
        WishCourse wishCourse = saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            false,
            WishAutoApplyResult.NOT_SELECTED,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPatch("/api/v1/student/wish-courses/" + wishCourse.getId() + "/auto-apply")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("WISH_011"));
    }

    @Test
    void autoApplyCheckMarksDoneAndUpdatesSummary() throws Exception {
        WishFixture fixture = saveWishFixture("check-done");
        saveWishlistSchedule(fixture.semester(), true);
        CourseOffering manualOffering = saveAdditionalOffering(
            fixture,
            "WISH102-check-done",
            "Operating Systems",
            30,
            DayOfWeek.WED,
            3,
            4
        );
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            manualOffering,
            false,
            WishAutoApplyResult.PENDING,
            "남아 있던 메시지"
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.wishCount").value(2))
            .andExpect(jsonPath("$.summary.autoApplySelectedCount").value(1))
            .andExpect(jsonPath("$.summary.autoDoneCount").value(1))
            .andExpect(jsonPath("$.summary.manualRequiredCount").value(1))
            .andExpect(jsonPath("$.items[0].result").value("DONE"))
            .andExpect(jsonPath("$.items[0].resultMessage").value("자동신청 가능 상태입니다."))
            .andExpect(jsonPath("$.items[1].result").value("NOT_SELECTED"))
            .andExpect(jsonPath("$.items[1].resultMessage").value("자동신청을 선택하지 않았습니다."));
    }

    @Test
    void autoApplyCheckMarksDoneWhenAllowedGradeDoesNotExist() throws Exception {
        WishFixture fixture = saveWishFixture("grade-none");
        saveWishlistSchedule(fixture.semester(), true);
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].result").value("DONE"))
            .andExpect(jsonPath("$.summary.autoDoneCount").value(1));
    }

    @Test
    void autoApplyCheckMarksDoneWhenStudentGradeIsAllowed() throws Exception {
        WishFixture fixture = saveWishFixture("grade-allowed");
        saveWishlistSchedule(fixture.semester(), true);
        saveAllowedGrade(fixture.courseOffering(), 2);
        saveAllowedGrade(fixture.courseOffering(), 3);
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].result").value("DONE"))
            .andExpect(jsonPath("$.summary.autoDoneCount").value(1));
    }

    @Test
    void autoApplyCheckMarksNeedsManualWhenStudentGradeIsNotAllowed() throws Exception {
        WishFixture fixture = saveWishFixture("grade-denied");
        saveWishlistSchedule(fixture.semester(), true);
        Integer originalCapacity = fixture.courseOffering().getCapacity();
        saveAllowedGrade(fixture.courseOffering(), 3);
        saveAllowedGrade(fixture.courseOffering(), 4);
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].result").value("NEEDS_MANUAL"))
            .andExpect(jsonPath("$.items[0].resultMessage").value(
                "신청 가능 학년이 아닙니다. 허용 학년: 3, 4학년"
            ))
            .andExpect(jsonPath("$.summary.autoDoneCount").value(0))
            .andExpect(jsonPath("$.summary.manualRequiredCount").value(1));

        Number enrollmentCount = (Number) entityManager
            .createNativeQuery("select count(*) from enrollments")
            .getSingleResult();
        assertThat(enrollmentCount.longValue()).isZero();
        assertThat(courseOfferingRepository.findById(fixture.courseOffering().getId()).orElseThrow()
            .getCapacity()).isEqualTo(originalCapacity);
    }

    @Test
    void gradeFailureTakesPriorityOverPrerequisiteTimeConflictAndCapacity() throws Exception {
        WishFixture fixture = saveWishFixture("grade-priority");
        saveWishlistSchedule(fixture.semester(), true);
        CourseOffering deniedPrerequisiteOffering = saveAdditionalOffering(
            fixture,
            "AAA-GRADE-PREREQ",
            "Grade Prerequisite",
            30,
            DayOfWeek.TUE,
            1,
            2
        );
        CourseOffering deniedCapacityOffering = saveAdditionalOffering(
            fixture,
            "AAB-GRADE-CAPACITY",
            "Grade Capacity",
            1,
            DayOfWeek.WED,
            1,
            2
        );
        CourseOffering deniedConflictOffering = saveAdditionalOffering(
            fixture,
            "AAC-GRADE-CONFLICT",
            "Grade Conflict",
            30,
            DayOfWeek.MON,
            1,
            2
        );
        CourseOffering conflictOffering = saveAdditionalOffering(
            fixture,
            "AAD-GRADE-CONFLICT",
            "Other Conflict",
            30,
            DayOfWeek.MON,
            2,
            3
        );
        Subject prerequisite = saveSubject(fixture, "PRE101-grade-priority", "자료구조");
        savePrerequisite(deniedPrerequisiteOffering.getSubject(), prerequisite, true);
        saveAllowedGrade(deniedPrerequisiteOffering, 3);
        saveAllowedGrade(deniedCapacityOffering, 3);
        saveAllowedGrade(deniedConflictOffering, 3);
        saveWishCourse(
            fixture.otherStudentProfile(),
            fixture.semester(),
            deniedCapacityOffering,
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            deniedPrerequisiteOffering,
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            deniedCapacityOffering,
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            deniedConflictOffering,
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            conflictOffering,
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].result").value("NEEDS_MANUAL"))
            .andExpect(jsonPath("$.items[0].resultMessage").value(
                "신청 가능 학년이 아닙니다. 허용 학년: 3학년"
            ))
            .andExpect(jsonPath("$.items[1].result").value("NEEDS_MANUAL"))
            .andExpect(jsonPath("$.items[2].result").value("NEEDS_MANUAL"));
    }

    @Test
    void autoApplyFalseCourseDoesNotCheckAllowedGrade() throws Exception {
        WishFixture fixture = saveWishFixture("grade-not-selected");
        saveWishlistSchedule(fixture.semester(), true);
        saveAllowedGrade(fixture.courseOffering(), 3);
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            false,
            WishAutoApplyResult.PENDING,
            "기존 메시지"
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].result").value("NOT_SELECTED"))
            .andExpect(jsonPath("$.items[0].resultMessage").value("자동신청을 선택하지 않았습니다."));
    }

    @Test
    void autoApplyCheckMarksDoneWhenPrerequisiteIsPassed() throws Exception {
        WishFixture fixture = saveWishFixture("prereq-passed");
        saveWishlistSchedule(fixture.semester(), true);
        Subject prerequisite = saveSubject(fixture, "PRE101-passed", "자료구조");
        savePrerequisite(fixture.courseOffering().getSubject(), prerequisite, true);
        saveCompletedCourse(fixture.studentProfile(), prerequisite, fixture.semester(), true);
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].result").value("DONE"))
            .andExpect(jsonPath("$.summary.autoDoneCount").value(1));
    }

    @Test
    void autoApplyCheckMarksNeedsManualWhenPrerequisiteIsMissing() throws Exception {
        WishFixture fixture = saveWishFixture("prereq-missing");
        saveWishlistSchedule(fixture.semester(), true);
        Subject prerequisite = saveSubject(fixture, "PRE101-missing", "자료구조");
        savePrerequisite(fixture.courseOffering().getSubject(), prerequisite, true);
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].result").value("NEEDS_MANUAL"))
            .andExpect(jsonPath("$.items[0].resultMessage").value("선수과목 이수 필요: 자료구조"))
            .andExpect(jsonPath("$.summary.manualRequiredCount").value(1));
    }

    @Test
    void autoApplyCheckListsOnlyMissingPrerequisitesWhenSomeArePassed() throws Exception {
        WishFixture fixture = saveWishFixture("prereq-partial");
        saveWishlistSchedule(fixture.semester(), true);
        Subject passedPrerequisite = saveSubject(fixture, "PRE101-partial", "자료구조");
        Subject missingPrerequisite = saveSubject(fixture, "PRE102-partial", "알고리즘");
        savePrerequisite(fixture.courseOffering().getSubject(), passedPrerequisite, true);
        savePrerequisite(fixture.courseOffering().getSubject(), missingPrerequisite, true);
        saveCompletedCourse(fixture.studentProfile(), passedPrerequisite, fixture.semester(), true);
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].result").value("NEEDS_MANUAL"))
            .andExpect(jsonPath("$.items[0].resultMessage").value("선수과목 이수 필요: 알고리즘"));
    }

    @Test
    void inactivePrerequisiteIsIgnoredAndFailedCompletedCourseDoesNotPass() throws Exception {
        WishFixture fixture = saveWishFixture("prereq-inactive");
        saveWishlistSchedule(fixture.semester(), true);
        Subject inactivePrerequisite = saveSubject(fixture, "PRE101-inactive", "자료구조");
        savePrerequisite(fixture.courseOffering().getSubject(), inactivePrerequisite, false);
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            true,
            WishAutoApplyResult.PENDING,
            null
        );

        CourseOffering failedOffering = saveAdditionalOffering(
            fixture,
            "ZZZ-FAILED-PREREQ",
            "Failed Prerequisite",
            30,
            DayOfWeek.WED,
            3,
            4
        );
        Subject failedPrerequisite = saveSubject(fixture, "PRE102-failed", "알고리즘");
        savePrerequisite(failedOffering.getSubject(), failedPrerequisite, true);
        saveCompletedCourse(fixture.studentProfile(), failedPrerequisite, fixture.semester(), false);
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            failedOffering,
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].result").value("DONE"))
            .andExpect(jsonPath("$.items[1].result").value("NEEDS_MANUAL"));
    }

    @Test
    void prerequisiteFailureTakesPriorityOverCapacityAndTimeConflict() throws Exception {
        WishFixture fixture = saveWishFixture("prereq-priority");
        saveWishlistSchedule(fixture.semester(), true);
        CourseOffering capacityOneOffering = saveAdditionalOffering(
            fixture,
            "AAA-PREREQ-PRIORITY",
            "Priority Capacity",
            1,
            DayOfWeek.MON,
            1,
            2
        );
        CourseOffering conflictOffering = saveAdditionalOffering(
            fixture,
            "AAB-PREREQ-PRIORITY",
            "Priority Conflict",
            30,
            DayOfWeek.MON,
            2,
            3
        );
        Subject prerequisite = saveSubject(fixture, "PRE101-priority", "자료구조");
        savePrerequisite(capacityOneOffering.getSubject(), prerequisite, true);
        saveWishCourse(
            fixture.otherStudentProfile(),
            fixture.semester(),
            capacityOneOffering,
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            capacityOneOffering,
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            conflictOffering,
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].result").value("NEEDS_MANUAL"))
            .andExpect(jsonPath("$.items[0].resultMessage").value("선수과목 이수 필요: 자료구조"))
            .andExpect(jsonPath("$.items[1].result").value("TIME_CONFLICT"));
    }

    @Test
    void autoApplyFalseCourseDoesNotCheckPrerequisite() throws Exception {
        WishFixture fixture = saveWishFixture("prereq-not-selected");
        saveWishlistSchedule(fixture.semester(), true);
        Subject prerequisite = saveSubject(fixture, "PRE101-not-selected", "자료구조");
        savePrerequisite(fixture.courseOffering().getSubject(), prerequisite, true);
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            false,
            WishAutoApplyResult.PENDING,
            "기존 메시지"
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].result").value("NOT_SELECTED"))
            .andExpect(jsonPath("$.items[0].resultMessage").value("자동신청을 선택하지 않았습니다."));
    }

    @Test
    void autoApplyCheckMarksOverCapacityByCreatedOrder() throws Exception {
        WishFixture fixture = saveWishFixture("check-capacity");
        saveWishlistSchedule(fixture.semester(), true);
        CourseOffering capacityOneOffering = saveAdditionalOffering(
            fixture,
            "AAA-CAPACITY",
            "Capacity Test",
            1,
            DayOfWeek.TUE,
            1,
            2
        );
        saveWishCourse(
            fixture.otherStudentProfile(),
            fixture.semester(),
            capacityOneOffering,
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            capacityOneOffering,
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].result").value("OVER_CAPACITY"))
            .andExpect(jsonPath("$.items[0].resultMessage").value("정원 초과로 본 신청이 필요합니다."))
            .andExpect(jsonPath("$.summary.autoDoneCount").value(0))
            .andExpect(jsonPath("$.summary.manualRequiredCount").value(1));
    }

    @Test
    void autoApplyCheckMarksTimeConflictForBothConflictingCourses() throws Exception {
        WishFixture fixture = saveWishFixture("check-conflict");
        saveWishlistSchedule(fixture.semester(), true);
        CourseOffering conflictOffering = saveAdditionalOffering(
            fixture,
            "WISH102-check-conflict",
            "Conflict Test",
            30,
            DayOfWeek.MON,
            2,
            3
        );
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            conflictOffering,
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].result").value("TIME_CONFLICT"))
            .andExpect(jsonPath("$.items[1].result").value("TIME_CONFLICT"))
            .andExpect(jsonPath("$.summary.manualRequiredCount").value(2));
    }

    @Test
    void timeConflictTakesPriorityOverOverCapacity() throws Exception {
        WishFixture fixture = saveWishFixture("check-priority");
        saveWishlistSchedule(fixture.semester(), true);
        CourseOffering capacityOneOffering = saveAdditionalOffering(
            fixture,
            "AAA-PRIORITY",
            "Priority Capacity",
            1,
            DayOfWeek.MON,
            1,
            2
        );
        CourseOffering conflictOffering = saveAdditionalOffering(
            fixture,
            "AAB-PRIORITY",
            "Priority Conflict",
            30,
            DayOfWeek.MON,
            2,
            3
        );
        saveWishCourse(
            fixture.otherStudentProfile(),
            fixture.semester(),
            capacityOneOffering,
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            capacityOneOffering,
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            conflictOffering,
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].result").value("TIME_CONFLICT"))
            .andExpect(jsonPath("$.items[1].result").value("TIME_CONFLICT"));
    }

    @Test
    void autoApplyCheckOutsideWishlistPeriodReturnsDomainError() throws Exception {
        WishFixture fixture = saveWishFixture("check-closed");
        saveWishlistSchedule(fixture.semester(), false);
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("WISH_007"));
    }

    @Test
    void autoApplyCheckUnauthorizedAndForbidden() throws Exception {
        WishFixture fixture = saveWishFixture("check-auth");
        saveWishlistSchedule(fixture.semester(), true);
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_005"));

        Cookie professorAccessToken = login(fixture.professor().getLoginId());
        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(professorAccessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("AUTH_006"));
    }

    @Test
    void autoApplyCheckDoesNotCreateEnrollmentOrChangeCapacity() throws Exception {
        WishFixture fixture = saveWishFixture("check-no-enrollment");
        saveWishlistSchedule(fixture.semester(), true);
        Integer originalCapacity = fixture.courseOffering().getCapacity();
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());
        Cookie csrfCookie = getCsrfCookie();

        mockMvc.perform(apiPost("/api/v1/student/wish-courses/auto-apply/check")
                .cookie(accessToken, csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"year":2026,"term":"FIRST"}
                    """))
            .andExpect(status().isOk());

        Number enrollmentCount = (Number) entityManager
            .createNativeQuery("select count(*) from enrollments")
            .getSingleResult();
        assertThat(enrollmentCount.longValue()).isZero();
        assertThat(courseOfferingRepository.findById(fixture.courseOffering().getId()).orElseThrow()
            .getCapacity()).isEqualTo(originalCapacity);
    }

    @Test
    void studentCanSearchWishCourseCandidates() throws Exception {
        WishFixture fixture = saveWishFixture("candidate");
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            true,
            WishAutoApplyResult.PENDING,
            "자동신청 대기 중"
        );
        saveWishCourse(
            fixture.otherStudentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            false,
            WishAutoApplyResult.NOT_SELECTED,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/wish-courses/candidates")
                .cookie(accessToken)
                .param("mode", "condition")
                .param("year", "2026")
                .param("term", "FIRST")
                .param("collegeName", "Engineering")
                .param("departmentName", "Computer Science")
                .param("category", "MAJOR_REQUIRED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courseOfferingId").value(fixture.courseOffering().getId()))
            .andExpect(jsonPath("$[0].subjectCode").value("WISH101-candidate"))
            .andExpect(jsonPath("$[0].courseTimeText").value("월 1~2교시"))
            .andExpect(jsonPath("$[0].wishCount").value(2))
            .andExpect(jsonPath("$[0].autoApplyCount").value(1))
            .andExpect(jsonPath("$[0].wished").value(true))
            .andExpect(jsonPath("$[0].autoApply").value(true))
            .andExpect(jsonPath("$[0].result").value("PENDING"))
            .andExpect(jsonPath("$[0].resultMessage").value("자동신청 대기 중"));
    }

    @Test
    void studentCanSearchWishCourseCandidatesByDirectInput() throws Exception {
        WishFixture fixture = saveWishFixture("direct");
        Cookie accessToken = login(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/wish-courses/candidates")
                .cookie(accessToken)
                .param("mode", "direct")
                .param("year", "2026")
                .param("term", "FIRST")
                .param("subjectName", "Data")
                .param("professorName", "Professor direct")
                .param("departmentName", "Computer"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courseOfferingId").value(fixture.courseOffering().getId()))
            .andExpect(jsonPath("$[0].wished").value(false))
            .andExpect(jsonPath("$[0].autoApply").value(false))
            .andExpect(jsonPath("$[0].result").doesNotExist());
    }

    @Test
    void studentCanGetMyWishCoursesWithSummary() throws Exception {
        WishFixture fixture = saveWishFixture("list");
        CourseOffering secondOffering = saveAdditionalOffering(fixture, "WISH102-list", "Operating Systems");
        CourseOffering thirdOffering = saveAdditionalOffering(fixture, "WISH103-list", "Algorithms");
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            secondOffering,
            true,
            WishAutoApplyResult.DONE,
            "자동신청 완료"
        );
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            thirdOffering,
            false,
            WishAutoApplyResult.NOT_SELECTED,
            null
        );
        saveWishCourse(
            fixture.otherStudentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/wish-courses")
                .cookie(accessToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.wishCount").value(3))
            .andExpect(jsonPath("$.summary.autoApplySelectedCount").value(2))
            .andExpect(jsonPath("$.summary.autoDoneCount").value(1))
            .andExpect(jsonPath("$.summary.manualRequiredCount").value(1))
            .andExpect(jsonPath("$.items.length()").value(3))
            .andExpect(jsonPath("$.items[0].wishCount").value(2))
            .andExpect(jsonPath("$.items[0].autoApplyCount").value(2));
    }

    @Test
    void myWishCoursesOnlyIncludeCurrentStudent() throws Exception {
        WishFixture fixture = saveWishFixture("owner");
        saveWishCourse(
            fixture.otherStudentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            true,
            WishAutoApplyResult.PENDING,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/wish-courses")
                .cookie(accessToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.wishCount").value(0))
            .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void unauthenticatedWishCourseRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(apiGet("/api/v1/student/wish-courses")
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_005"));
    }

    @Test
    void professorWishCourseRequestReturnsForbidden() throws Exception {
        WishFixture fixture = saveWishFixture("forbidden");
        Cookie accessToken = login(fixture.professor().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/wish-courses")
                .cookie(accessToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("AUTH_006"));
    }

    @Test
    void missingYearReturnsWishCourseDomainError() throws Exception {
        WishFixture fixture = saveWishFixture("missing-year");
        Cookie accessToken = login(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/wish-courses/candidates")
                .cookie(accessToken)
                .param("term", "FIRST"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("WISH_002"))
            .andExpect(jsonPath("$.message").value("학년도는 필수입니다."));
    }

    @Test
    void missingTermReturnsWishCourseDomainError() throws Exception {
        WishFixture fixture = saveWishFixture("missing-term");
        Cookie accessToken = login(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/wish-courses")
                .cookie(accessToken)
                .param("year", "2026"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("WISH_003"))
            .andExpect(jsonPath("$.message").value("학기 구분은 필수입니다."));
    }

    @Test
    void invalidModeReturnsWishCourseDomainError() throws Exception {
        WishFixture fixture = saveWishFixture("invalid-mode");
        Cookie accessToken = login(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/wish-courses/candidates")
                .cookie(accessToken)
                .param("mode", "wrong")
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("WISH_001"))
            .andExpect(jsonPath("$.message").value("검색 모드는 condition 또는 direct만 사용할 수 있습니다."));
    }

    @Test
    void noMatchingCandidateReturnsEmptyArray() throws Exception {
        WishFixture fixture = saveWishFixture("empty-candidate");
        Cookie accessToken = login(fixture.student().getLoginId());

        MvcResult result = mockMvc.perform(apiGet("/api/v1/student/wish-courses/candidates")
                .cookie(accessToken)
                .param("year", "2026")
                .param("term", "FIRST")
                .param("departmentName", "Unknown Department"))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("[]");
    }

    @Test
    void openApiDocsExposeWishCourseEndpoints() throws Exception {
        mockMvc.perform(apiGet("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/student/wish-courses']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/student/wish-courses/candidates']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/student/wish-courses/auto-apply/check']").exists());
    }

    @Test
    void wishCourseReadApisDoNotCreateEnrollmentRows() throws Exception {
        WishFixture fixture = saveWishFixture("no-enrollment");
        saveWishCourse(
            fixture.studentProfile(),
            fixture.semester(),
            fixture.courseOffering(),
            false,
            WishAutoApplyResult.NOT_SELECTED,
            null
        );
        Cookie accessToken = login(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/wish-courses")
                .cookie(accessToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk());
        mockMvc.perform(apiGet("/api/v1/student/wish-courses/candidates")
                .cookie(accessToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk());

        Number count = (Number) entityManager
            .createNativeQuery("select count(*) from enrollments")
            .getSingleResult();
        assertThat(count.longValue()).isZero();
    }

    private WishFixture saveWishFixture(String suffix) {
        Department department = departmentRepository.saveAndFlush(
            Department.create("Computer Science", "Engineering", DepartmentType.DEPARTMENT)
        );
        Member student = saveMember("wish-student-" + suffix, "Kim Student " + suffix, MemberRole.STUDENT);
        StudentProfile studentProfile = studentProfileRepository.saveAndFlush(StudentProfile.create(
            student,
            "WS-" + suffix,
            department,
            2,
            AcademicStatus.ENROLLED,
            18
        ));
        Member otherStudent = saveMember(
            "wish-other-student-" + suffix,
            "Other Student " + suffix,
            MemberRole.STUDENT
        );
        StudentProfile otherStudentProfile = studentProfileRepository.saveAndFlush(StudentProfile.create(
            otherStudent,
            "WOS-" + suffix,
            department,
            3,
            AcademicStatus.ENROLLED,
            18
        ));
        Member professor = saveMember(
            "wish-professor-" + suffix,
            "Park Professor " + suffix,
            MemberRole.PROFESSOR
        );
        ProfessorProfile professorProfile = professorProfileRepository.saveAndFlush(
            ProfessorProfile.create(professor, "WP-" + suffix, department, "Professor")
        );
        Member staff = saveMember("wish-staff-" + suffix, "Staff " + suffix, MemberRole.STAFF);
        StaffProfile staffProfile = staffProfileRepository.saveAndFlush(
            StaffProfile.create(staff, "WT-" + suffix, department, "Staff", "Courses")
        );
        Semester semester = semesterRepository.saveAndFlush(
            Semester.create(2026, SemesterTerm.FIRST, "2026 FIRST", 18)
        );
        Subject subject = subjectRepository.saveAndFlush(Subject.create(
            "WISH101-" + suffix,
            "Data Structures",
            department,
            3,
            SubjectCategory.MAJOR_REQUIRED
        ));
        CourseOffering courseOffering = courseOfferingRepository.saveAndFlush(CourseOffering.create(
            semester,
            subject,
            professorProfile,
            30,
            CourseOfferingStatus.OPEN,
            staffProfile
        ));
        courseTimeRepository.saveAndFlush(CourseTime.create(
            courseOffering,
            DayOfWeek.MON,
            1,
            2
        ));

        return new WishFixture(
            student,
            professor,
            studentProfile,
            otherStudentProfile,
            semester,
            courseOffering
        );
    }

    private CourseOffering saveAdditionalOffering(
        WishFixture fixture,
        String subjectCode,
        String subjectName
    ) {
        return saveAdditionalOffering(
            fixture,
            subjectCode,
            subjectName,
            40,
            DayOfWeek.WED,
            3,
            4
        );
    }

    private CourseOffering saveAdditionalOffering(
        WishFixture fixture,
        String subjectCode,
        String subjectName,
        int capacity,
        DayOfWeek dayOfWeek,
        int startPeriod,
        int endPeriod
    ) {
        Department department = fixture.courseOffering().getSubject().getDepartment();
        Subject subject = subjectRepository.saveAndFlush(Subject.create(
            subjectCode,
            subjectName,
            department,
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

    private WishCourse saveWishCourse(
        StudentProfile student,
        Semester semester,
        CourseOffering courseOffering,
        boolean autoApply,
        WishAutoApplyResult result,
        String resultMessage
    ) {
        return wishCourseRepository.saveAndFlush(WishCourse.create(
            student,
            semester,
            courseOffering,
            autoApply,
            result,
            resultMessage
        ));
    }

    private Subject saveSubject(WishFixture fixture, String subjectCode, String subjectName) {
        return subjectRepository.saveAndFlush(Subject.create(
            subjectCode,
            subjectName,
            fixture.courseOffering().getSubject().getDepartment(),
            3,
            SubjectCategory.MAJOR_REQUIRED
        ));
    }

    private CoursePrerequisite savePrerequisite(
        Subject subject,
        Subject prerequisiteSubject,
        boolean active
    ) {
        return coursePrerequisiteRepository.saveAndFlush(CoursePrerequisite.create(
            subject,
            prerequisiteSubject,
            active
        ));
    }

    private CourseAllowedGrade saveAllowedGrade(CourseOffering courseOffering, Integer gradeLevel) {
        return courseAllowedGradeRepository.saveAndFlush(CourseAllowedGrade.create(
            courseOffering,
            gradeLevel
        ));
    }

    private CompletedCourse saveCompletedCourse(
        StudentProfile student,
        Subject subject,
        Semester semester,
        boolean passed
    ) {
        return completedCourseRepository.saveAndFlush(CompletedCourse.create(
            student,
            subject,
            semester,
            passed ? "A+" : "F",
            passed,
            LocalDateTime.now().minusDays(1)
        ));
    }

    private SemesterSchedule saveWishlistSchedule(Semester semester, boolean open) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = open ? now.minusDays(1) : now.minusDays(3);
        LocalDateTime endAt = open ? now.plusDays(1) : now.minusDays(1);
        return semesterScheduleRepository.saveAndFlush(SemesterSchedule.create(
            semester,
            SemesterScheduleType.WISHLIST,
            startAt,
            endAt,
            null,
            false
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

    private MockHttpServletRequestBuilder apiPatch(String path) {
        return patch(path).servletPath(path);
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

    private record WishFixture(
        Member student,
        Member professor,
        StudentProfile studentProfile,
        StudentProfile otherStudentProfile,
        Semester semester,
        CourseOffering courseOffering
    ) {
    }
}
