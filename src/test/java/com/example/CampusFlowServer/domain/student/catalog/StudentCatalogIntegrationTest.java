package com.example.CampusFlowServer.domain.student.catalog;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.example.CampusFlowServer.domain.courseplan.entity.CoursePlan;
import com.example.CampusFlowServer.domain.courseplan.entity.CoursePlanContent;
import com.example.CampusFlowServer.domain.courseplan.enums.CoursePlanStatus;
import com.example.CampusFlowServer.domain.courseplan.enums.CoursePlanVersionType;
import com.example.CampusFlowServer.domain.courseplan.repository.CoursePlanContentRepository;
import com.example.CampusFlowServer.domain.courseplan.repository.CoursePlanRepository;
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
import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import com.example.CampusFlowServer.domain.semester.repository.SemesterRepository;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StudentCatalogIntegrationTest {

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
    private SubjectRepository subjectRepository;

    @Autowired
    private CourseOfferingRepository courseOfferingRepository;

    @Autowired
    private CourseTimeRepository courseTimeRepository;

    @Autowired
    private CoursePlanRepository coursePlanRepository;

    @Autowired
    private CoursePlanContentRepository coursePlanContentRepository;

    @Test
    void studentCanSearchCatalogCoursesByCondition() throws Exception {
        CatalogFixture fixture = saveCatalogFixture("condition");
        Cookie accessToken = loginAsStudent(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/catalog/courses")
                .cookie(accessToken)
                .param("mode", "condition")
                .param("year", "2026")
                .param("term", "FIRST")
                .param("collegeName", "Engineering")
                .param("departmentName", "Computer Science")
                .param("category", "MAJOR_REQUIRED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courseOfferingId").value(fixture.courseOffering().getId()))
            .andExpect(jsonPath("$[0].subjectCode").value("CSE101-condition"))
            .andExpect(jsonPath("$[0].subjectName").value("Data Structures"))
            .andExpect(jsonPath("$[0].departmentName").value("Computer Science"))
            .andExpect(jsonPath("$[0].collegeName").value("Engineering"))
            .andExpect(jsonPath("$[0].category").value("MAJOR_REQUIRED"))
            .andExpect(jsonPath("$[0].professorName").value("Park Professor condition"))
            .andExpect(jsonPath("$[0].credit").value(3))
            .andExpect(jsonPath("$[0].courseTimeText").value("월 1~2교시, 수 3~4교시"))
            .andExpect(jsonPath("$[0].capacity").value(30))
            .andExpect(jsonPath("$[0].coursePlanStatus").value("PUBLISHED"))
            .andExpect(jsonPath("$[0].hasPublishedCoursePlan").value(true));
    }

    @Test
    void studentCanSearchCatalogCoursesByDirectInput() throws Exception {
        CatalogFixture fixture = saveCatalogFixture("direct");
        Cookie accessToken = loginAsStudent(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/catalog/courses")
                .cookie(accessToken)
                .param("mode", "direct")
                .param("year", "2026")
                .param("term", "FIRST")
                .param("subjectName", "Data")
                .param("professorName", "Professor direct")
                .param("departmentName", "Computer"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courseOfferingId").value(fixture.courseOffering().getId()))
            .andExpect(jsonPath("$[0].subjectName").value("Data Structures"))
            .andExpect(jsonPath("$[0].hasPublishedCoursePlan").value(true));
    }

    @Test
    void unauthenticatedCatalogRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(apiGet("/api/v1/student/catalog/courses")
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_005"));
    }

    @Test
    void professorCatalogRequestReturnsForbidden() throws Exception {
        CatalogFixture fixture = saveCatalogFixture("forbidden");
        Cookie professorAccessToken = login(fixture.professor().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/catalog/courses")
                .cookie(professorAccessToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("AUTH_006"));
    }

    @Test
    void noMatchingConditionReturnsEmptyArray() throws Exception {
        CatalogFixture fixture = saveCatalogFixture("empty");
        Cookie accessToken = loginAsStudent(fixture.student().getLoginId());

        MvcResult result = mockMvc.perform(apiGet("/api/v1/student/catalog/courses")
                .cookie(accessToken)
                .param("mode", "condition")
                .param("year", "2026")
                .param("term", "FIRST")
                .param("collegeName", "all")
                .param("departmentName", "Unknown Department"))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("[]");
    }

    @Test
    void catalogCanBeSearchedWithoutEnrollmentScheduleData() throws Exception {
        CatalogFixture fixture = saveCatalogFixture("always");
        Cookie accessToken = loginAsStudent(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/catalog/courses")
                .cookie(accessToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courseOfferingId").value(fixture.courseOffering().getId()));
    }

    @Test
    void catalogCoursesAreSortedBySubjectCodeAndCourseOfferingId() throws Exception {
        CatalogFixture fixture = saveCatalogFixture("sort");
        saveAdditionalCourseOffering(fixture, "AAA101-sort");
        Cookie accessToken = loginAsStudent(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/catalog/courses")
                .cookie(accessToken)
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].subjectCode").value("AAA101-sort"))
            .andExpect(jsonPath("$[1].subjectCode").value("CSE101-sort"));
    }

    @Test
    void missingYearReturnsCatalogRequiredYearError() throws Exception {
        CatalogFixture fixture = saveCatalogFixture("missing-year");
        Cookie accessToken = loginAsStudent(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/catalog/courses")
                .cookie(accessToken)
                .param("term", "FIRST"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("CATALOG_002"))
            .andExpect(jsonPath("$.message").value("학년도는 필수입니다."));
    }

    @Test
    void missingTermReturnsCatalogRequiredTermError() throws Exception {
        CatalogFixture fixture = saveCatalogFixture("missing-term");
        Cookie accessToken = loginAsStudent(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/catalog/courses")
                .cookie(accessToken)
                .param("year", "2026"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("CATALOG_003"))
            .andExpect(jsonPath("$.message").value("학기 구분은 필수입니다."));
    }

    @Test
    void invalidModeReturnsCatalogInvalidSearchModeError() throws Exception {
        CatalogFixture fixture = saveCatalogFixture("invalid-mode");
        Cookie accessToken = loginAsStudent(fixture.student().getLoginId());

        mockMvc.perform(apiGet("/api/v1/student/catalog/courses")
                .cookie(accessToken)
                .param("mode", "wrong")
                .param("year", "2026")
                .param("term", "FIRST"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("CATALOG_001"))
            .andExpect(jsonPath("$.message").value("검색 모드는 condition 또는 direct만 사용할 수 있습니다."));
    }

    @Test
    void openApiDocsExposeStudentCatalogCourseEndpoint() throws Exception {
        mockMvc.perform(apiGet("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/student/catalog/courses']").exists());
    }

    private CatalogFixture saveCatalogFixture(String suffix) {
        Department department = departmentRepository.saveAndFlush(
            Department.create("Computer Science", "Engineering", DepartmentType.DEPARTMENT)
        );
        Member student = saveMember("student-" + suffix, "Kim Student " + suffix, MemberRole.STUDENT);
        studentProfileRepository.saveAndFlush(StudentProfile.create(
            student,
            "S-" + suffix,
            department,
            2,
            AcademicStatus.ENROLLED,
            18
        ));

        Member professor = saveMember(
            "professor-" + suffix,
            "Park Professor " + suffix,
            MemberRole.PROFESSOR
        );
        ProfessorProfile professorProfile = professorProfileRepository.saveAndFlush(
            ProfessorProfile.create(professor, "P-" + suffix, department, "Professor")
        );

        Member staff = saveMember("staff-" + suffix, "Staff " + suffix, MemberRole.STAFF);
        StaffProfile staffProfile = staffProfileRepository.saveAndFlush(
            StaffProfile.create(staff, "T-" + suffix, department, "Staff", "Courses")
        );

        Semester semester = semesterRepository.saveAndFlush(
            Semester.create(2026, SemesterTerm.FIRST, "2026 FIRST", 18)
        );
        Subject subject = subjectRepository.saveAndFlush(Subject.create(
            "CSE101-" + suffix,
            "Data Structures",
            department,
            3,
            SubjectCategory.MAJOR_REQUIRED
        ));
        CourseOffering courseOffering = courseOfferingRepository.saveAndFlush(
            CourseOffering.create(
                semester,
                subject,
                professorProfile,
                30,
                CourseOfferingStatus.OPEN,
                staffProfile
            )
        );
        courseTimeRepository.saveAllAndFlush(List.of(
            CourseTime.create(courseOffering, DayOfWeek.MON, 1, 2),
            CourseTime.create(courseOffering, DayOfWeek.WED, 3, 4)
        ));
        CoursePlan coursePlan = saveCoursePlan(courseOffering, professorProfile);
        savePublishedCoursePlanContent(coursePlan);

        return new CatalogFixture(student, professor, courseOffering);
    }

    private CourseOffering saveAdditionalCourseOffering(CatalogFixture fixture, String subjectCode) {
        CourseOffering baseCourseOffering = fixture.courseOffering();
        Subject subject = subjectRepository.saveAndFlush(Subject.create(
            subjectCode,
            "Algorithms",
            baseCourseOffering.getSubject().getDepartment(),
            3,
            SubjectCategory.MAJOR_REQUIRED
        ));

        return courseOfferingRepository.saveAndFlush(CourseOffering.create(
            baseCourseOffering.getSemester(),
            subject,
            baseCourseOffering.getProfessor(),
            30,
            CourseOfferingStatus.OPEN,
            baseCourseOffering.getCreatedBy()
        ));
    }

    private CoursePlan saveCoursePlan(
        CourseOffering courseOffering,
        ProfessorProfile professorProfile
    ) {
        CoursePlan coursePlan = BeanUtils.instantiateClass(CoursePlan.class);
        ReflectionTestUtils.setField(coursePlan, "courseOffering", courseOffering);
        ReflectionTestUtils.setField(coursePlan, "status", CoursePlanStatus.PUBLISHED);
        ReflectionTestUtils.setField(coursePlan, "createdBy", professorProfile);
        ReflectionTestUtils.setField(coursePlan, "publishedAt", LocalDateTime.now());
        return coursePlanRepository.saveAndFlush(coursePlan);
    }

    private CoursePlanContent savePublishedCoursePlanContent(CoursePlan coursePlan) {
        CoursePlanContent content = BeanUtils.instantiateClass(CoursePlanContent.class);
        ReflectionTestUtils.setField(content, "coursePlan", coursePlan);
        ReflectionTestUtils.setField(content, "versionType", CoursePlanVersionType.PUBLISHED);
        ReflectionTestUtils.setField(content, "overview", "Overview");
        return coursePlanContentRepository.saveAndFlush(content);
    }

    private Member saveMember(String loginId, String name, MemberRole role) {
        Member member = Member.create(
            loginId,
            passwordEncoder.encode(PASSWORD),
            name,
            role
        );
        return memberRepository.saveAndFlush(member);
    }

    private Cookie loginAsStudent(String loginId) throws Exception {
        return login(loginId);
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

    private record CatalogFixture(
        Member student,
        Member professor,
        CourseOffering courseOffering
    ) {
    }
}
