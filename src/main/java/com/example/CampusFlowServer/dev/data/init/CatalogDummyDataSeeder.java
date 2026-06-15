package com.example.CampusFlowServer.dev.data.init;

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
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("catalog-seed")
@RequiredArgsConstructor
public class CatalogDummyDataSeeder implements ApplicationRunner {

    private static final String RAW_PASSWORD = "1234";
    private static final int STUDENT_COUNT = 30000;
    private static final int PROFESSOR_COUNT = 100;
    private static final int STAFF_COUNT = 20;
    private static final int SUBJECT_COUNT = 500;
    private static final int OFFERING_COUNT = 500;
    private static final int STUDENT_CHUNK_SIZE = 1000;

    private static final DepartmentSeed[] DEPARTMENT_SEEDS = {
        new DepartmentSeed("컴퓨터공학과", "공과대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("정보컴퓨터공학부", "공과대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("전자공학과", "공과대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("기계공학과", "공과대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("산업공학과", "공과대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("건축공학과", "공과대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("화학공학과", "공과대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("경영학과", "경영대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("회계학과", "경영대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("경제학과", "사회과학대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("행정학과", "사회과학대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("심리학과", "사회과학대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("국어국문학과", "인문대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("영어영문학과", "인문대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("사학과", "인문대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("철학과", "인문대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("수학과", "자연과학대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("물리학과", "자연과학대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("화학과", "자연과학대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("생명과학과", "자연과학대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("통계학과", "자연과학대학", DepartmentType.DEPARTMENT),
        new DepartmentSeed("교양교육원", "교양교육원", DepartmentType.GENERAL_EDUCATION),
        new DepartmentSeed("학사지원과", "대학본부", DepartmentType.OFFICE),
        new DepartmentSeed("입학처", "대학본부", DepartmentType.OFFICE),
        new DepartmentSeed("학생지원과", "대학본부", DepartmentType.OFFICE)
    };

    private static final String[] SUBJECT_NAMES = {
        "자료구조",
        "운영체제",
        "데이터베이스",
        "알고리즘",
        "컴퓨터네트워크",
        "소프트웨어공학",
        "웹프로그래밍",
        "객체지향프로그래밍",
        "인공지능개론",
        "분산시스템",
        "컴퓨터구조",
        "경영학원론",
        "회계원리",
        "경제학원론",
        "통계학",
        "창의적사고와글쓰기",
        "대학영어",
        "선형대수학",
        "일반물리학",
        "일반화학"
    };

    private static final String[] POSITIONS = {"교수", "부교수", "조교수"};
    private static final DayOfWeek[] COURSE_DAYS = {
        DayOfWeek.MON,
        DayOfWeek.TUE,
        DayOfWeek.WED,
        DayOfWeek.THU,
        DayOfWeek.FRI
    };
    private static final SubjectCategory[] CATEGORIES = SubjectCategory.values();

    private final DepartmentRepository departmentRepository;
    private final MemberRepository memberRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ProfessorProfileRepository professorProfileRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseTimeRepository courseTimeRepository;
    private final CoursePlanRepository coursePlanRepository;
    private final CoursePlanContentRepository coursePlanContentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info(
            "CatalogDummyDataSeeder started. students={}, professors={}, staff={}, subjects={}, offerings={}",
            STUDENT_COUNT,
            PROFESSOR_COUNT,
            STAFF_COUNT,
            SUBJECT_COUNT,
            OFFERING_COUNT
        );

        SeedStats stats = new SeedStats();
        String encodedPassword = passwordEncoder.encode(RAW_PASSWORD);

        List<Department> departments = seedDepartments(stats);
        List<Department> academicDepartments = departments.stream()
            .filter(department -> DepartmentType.DEPARTMENT.equals(department.getType())
                || DepartmentType.GENERAL_EDUCATION.equals(department.getType()))
            .toList();
        List<Department> offices = departments.stream()
            .filter(department -> DepartmentType.OFFICE.equals(department.getType()))
            .toList();

        seedStudents(academicDepartments, encodedPassword, stats);
        List<ProfessorProfile> professors = seedProfessors(academicDepartments, encodedPassword, stats);
        List<StaffProfile> staff = seedStaff(offices, encodedPassword, stats);
        List<Semester> semesters = seedSemesters(stats);
        List<Subject> subjects = seedSubjects(academicDepartments, stats);
        List<CourseOffering> offerings = seedCourseOfferings(subjects, semesters, professors, staff, stats);
        seedCourseTimes(offerings, stats);
        seedCoursePlans(offerings, stats);

        log.info(
            "CatalogDummyDataSeeder completed. departments={}, students={}, professors={}, staff={}, semesters={}, subjects={}, offerings={}, courseTimes={}, coursePlans={}, coursePlanContents={}, created={}, reused={}",
            departments.size(),
            STUDENT_COUNT,
            PROFESSOR_COUNT,
            STAFF_COUNT,
            semesters.size(),
            subjects.size(),
            offerings.size(),
            stats.courseTimesCreated,
            stats.coursePlansCreated + stats.coursePlansReused,
            stats.coursePlanContentsCreated,
            stats.createdTotal(),
            stats.reusedTotal()
        );
    }

    private List<Department> seedDepartments(SeedStats stats) {
        List<Department> departments = new ArrayList<>();
        for (DepartmentSeed seed : DEPARTMENT_SEEDS) {
            Department department = departmentRepository.findByName(seed.name())
                .map(existing -> {
                    stats.departmentsReused++;
                    return existing;
                })
                .orElseGet(() -> {
                    stats.departmentsCreated++;
                    return departmentRepository.save(
                        Department.create(seed.name(), seed.collegeName(), seed.type())
                    );
                });
            departments.add(department);
        }
        return departments;
    }

    private void seedStudents(
        List<Department> departments,
        String encodedPassword,
        SeedStats stats
    ) {
        for (int start = 1; start <= STUDENT_COUNT; start += STUDENT_CHUNK_SIZE) {
            int end = Math.min(start + STUDENT_CHUNK_SIZE - 1, STUDENT_COUNT);
            seedStudentChunk(start, end, departments, encodedPassword, stats);
        }
    }

    private void seedStudentChunk(
        int start,
        int end,
        List<Department> departments,
        String encodedPassword,
        SeedStats stats
    ) {
        List<String> loginIds = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            loginIds.add(studentLoginId(i));
        }

        Map<String, Member> membersByLoginId = memberRepository.findByLoginIdIn(loginIds)
            .stream()
            .collect(Collectors.toMap(Member::getLoginId, Function.identity()));
        stats.membersReused += membersByLoginId.size();

        List<Member> newMembers = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            String loginId = studentLoginId(i);
            if (membersByLoginId.containsKey(loginId)) {
                continue;
            }

            newMembers.add(Member.create(
                loginId,
                encodedPassword,
                "학생%05d".formatted(i),
                MemberRole.STUDENT
            ));
        }
        if (!newMembers.isEmpty()) {
            memberRepository.saveAll(newMembers).forEach(member ->
                membersByLoginId.put(member.getLoginId(), member)
            );
            stats.membersCreated += newMembers.size();
        }

        Map<String, StudentProfile> profilesByLoginId = studentProfileRepository
            .findByMember_LoginIdIn(loginIds)
            .stream()
            .collect(Collectors.toMap(
                profile -> profile.getMember().getLoginId(),
                Function.identity()
            ));
        stats.studentProfilesReused += profilesByLoginId.size();

        List<StudentProfile> newProfiles = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            String loginId = studentLoginId(i);
            if (profilesByLoginId.containsKey(loginId)) {
                continue;
            }

            Department department = departments.get((i - 1) % departments.size());
            int grade = ((i - 1) % 4) + 1;
            newProfiles.add(StudentProfile.create(
                membersByLoginId.get(loginId),
                "S2026%05d".formatted(i),
                department,
                grade,
                AcademicStatus.ENROLLED,
                18
            ));
        }
        if (!newProfiles.isEmpty()) {
            studentProfileRepository.saveAll(newProfiles);
            stats.studentProfilesCreated += newProfiles.size();
        }
    }

    private List<ProfessorProfile> seedProfessors(
        List<Department> departments,
        String encodedPassword,
        SeedStats stats
    ) {
        List<ProfessorProfile> professors = new ArrayList<>();
        for (int i = 1; i <= PROFESSOR_COUNT; i++) {
            Department department = departments.get((i - 1) % departments.size());
            ProfessorProfile professor = ensureProfessorProfile(
                "prof%03d".formatted(i),
                "교수%03d".formatted(i),
                "P%04d".formatted(i),
                department,
                POSITIONS[(i - 1) % POSITIONS.length],
                encodedPassword,
                stats
            );
            if (!professors.contains(professor)) {
                professors.add(professor);
            }
        }
        return professors.stream().limit(PROFESSOR_COUNT).toList();
    }

    private List<StaffProfile> seedStaff(
        List<Department> offices,
        String encodedPassword,
        SeedStats stats
    ) {
        List<StaffProfile> staff = new ArrayList<>();
        for (int i = 1; i <= STAFF_COUNT; i++) {
            StaffProfile staffProfile = ensureStaffProfile(
                "staff%03d".formatted(i),
                "교직원%03d".formatted(i),
                "T%04d".formatted(i),
                offices.get((i - 1) % offices.size()),
                "직원",
                "학사 행정",
                encodedPassword,
                stats
            );
            if (!staff.contains(staffProfile)) {
                staff.add(staffProfile);
            }
        }
        return staff.stream().limit(STAFF_COUNT).toList();
    }

    private List<Semester> seedSemesters(SeedStats stats) {
        int[][] semesterSeeds = {
            {2024, SemesterTerm.FIRST.ordinal()},
            {2024, SemesterTerm.SECOND.ordinal()},
            {2025, SemesterTerm.FIRST.ordinal()},
            {2025, SemesterTerm.SECOND.ordinal()},
            {2026, SemesterTerm.FIRST.ordinal()},
            {2026, SemesterTerm.SECOND.ordinal()},
            {2027, SemesterTerm.FIRST.ordinal()}
        };

        List<Semester> semesters = new ArrayList<>();
        for (int[] seed : semesterSeeds) {
            SemesterTerm term = SemesterTerm.values()[seed[1]];
            Semester semester = semesterRepository.findByYearAndTerm(seed[0], term)
                .map(existing -> {
                    stats.semestersReused++;
                    return existing;
                })
                .orElseGet(() -> {
                    stats.semestersCreated++;
                    return semesterRepository.save(
                        Semester.create(seed[0], term, "%d학년도 %s".formatted(seed[0], term.name()), 18)
                    );
                });
            semesters.add(semester);
        }
        return semesters;
    }

    private List<Subject> seedSubjects(List<Department> departments, SeedStats stats) {
        List<Subject> subjects = new ArrayList<>();
        for (int i = 0; i < SUBJECT_COUNT; i++) {
            SubjectSeed seed = subjectSeed(i);
            Department department = departments.get(i % departments.size());
            SubjectCategory category = categoryFor(seed.code(), i);
            int credit = 2 + (i % 3);
            Subject subject = subjectRepository.findByCode(seed.code())
                .map(existing -> {
                    stats.subjectsReused++;
                    return existing;
                })
                .orElseGet(() -> {
                    stats.subjectsCreated++;
                    return subjectRepository.save(Subject.create(
                        seed.code(),
                        seed.name(),
                        department,
                        credit,
                        category
                    ));
                });
            subjects.add(subject);
        }
        return subjects;
    }

    private List<CourseOffering> seedCourseOfferings(
        List<Subject> subjects,
        List<Semester> semesters,
        List<ProfessorProfile> professors,
        List<StaffProfile> staff,
        SeedStats stats
    ) {
        Semester first2026 = semesters.stream()
            .filter(semester -> semester.getYear() == 2026
                && SemesterTerm.FIRST.equals(semester.getTerm()))
            .findFirst()
            .orElseThrow();

        List<CourseOffering> offerings = new ArrayList<>();
        for (int i = 0; i < OFFERING_COUNT; i++) {
            Subject subject = subjects.get(i);
            Semester semester = i < 400
                ? first2026
                : semesters.get((i - 400) % semesters.size());
            ProfessorProfile professor = professors.get(i % professors.size());
            StaffProfile createdBy = staff.get(i % staff.size());
            int capacity = 20 + ((i * 7) % 81);
            CourseOfferingStatus status = statusFor(i);

            CourseOffering offering = courseOfferingRepository
                .findBySemesterIdAndSubjectId(semester.getId(), subject.getId())
                .map(existing -> {
                    stats.offeringsReused++;
                    return existing;
                })
                .orElseGet(() -> {
                    stats.offeringsCreated++;
                    return courseOfferingRepository.save(CourseOffering.create(
                        semester,
                        subject,
                        professor,
                        capacity,
                        status,
                        createdBy
                    ));
                });
            offerings.add(offering);
        }
        return offerings;
    }

    private void seedCourseTimes(List<CourseOffering> offerings, SeedStats stats) {
        for (int i = 0; i < offerings.size(); i++) {
            CourseOffering offering = offerings.get(i);
            createCourseTimeIfAbsent(
                offering,
                COURSE_DAYS[i % COURSE_DAYS.length],
                (i % 8) + 1,
                (i % 8) + 2,
                stats
            );

            if (i % 3 == 0) {
                int startPeriod = ((i + 3) % 8) + 1;
                createCourseTimeIfAbsent(
                    offering,
                    COURSE_DAYS[(i + 2) % COURSE_DAYS.length],
                    startPeriod,
                    startPeriod + 1,
                    stats
                );
            }
        }
    }

    private void seedCoursePlans(List<CourseOffering> offerings, SeedStats stats) {
        for (int i = 0; i < offerings.size(); i++) {
            CourseOffering offering = offerings.get(i);
            if (i % 5 == 4) {
                continue;
            }

            CoursePlanStatus status = i % 5 == 3
                ? CoursePlanStatus.DRAFT
                : CoursePlanStatus.PUBLISHED;
            CoursePlan plan = findCoursePlan(offering)
                .map(existing -> {
                    stats.coursePlansReused++;
                    return existing;
                })
                .orElseGet(() -> {
                    stats.coursePlansCreated++;
                    CoursePlan coursePlan = BeanUtils.instantiateClass(CoursePlan.class);
                    setField(coursePlan, "courseOffering", offering);
                    setField(coursePlan, "status", status);
                    setField(coursePlan, "createdBy", offering.getProfessor());
                    if (CoursePlanStatus.PUBLISHED.equals(status)) {
                        setField(coursePlan, "publishedAt", LocalDateTime.now());
                    }
                    return coursePlanRepository.save(coursePlan);
                });

            CoursePlanVersionType versionType = CoursePlanStatus.PUBLISHED.equals(plan.getStatus())
                ? CoursePlanVersionType.PUBLISHED
                : CoursePlanVersionType.DRAFT;
            createCoursePlanContentIfAbsent(plan, versionType, stats);
        }
    }

    private Member ensureMember(
        String loginId,
        String name,
        MemberRole role,
        String encodedPassword,
        SeedStats stats
    ) {
        return memberRepository.findByLoginId(loginId)
            .map(existing -> {
                stats.membersReused++;
                return existing;
            })
            .orElseGet(() -> {
                stats.membersCreated++;
                return memberRepository.save(
                    Member.create(loginId, encodedPassword, name, role)
                );
            });
    }

    private ProfessorProfile ensureProfessorProfile(
        String loginId,
        String name,
        String professorNo,
        Department department,
        String position,
        String encodedPassword,
        SeedStats stats
    ) {
        Member member = ensureMember(loginId, name, MemberRole.PROFESSOR, encodedPassword, stats);
        return professorProfileRepository.findByMember_LoginId(loginId)
            .map(existing -> {
                stats.professorProfilesReused++;
                return existing;
            })
            .orElseGet(() -> {
                stats.professorProfilesCreated++;
                return professorProfileRepository.save(
                    ProfessorProfile.create(member, professorNo, department, position)
                );
            });
    }

    private StaffProfile ensureStaffProfile(
        String loginId,
        String name,
        String staffNo,
        Department department,
        String position,
        String responsibility,
        String encodedPassword,
        SeedStats stats
    ) {
        Member member = ensureMember(loginId, name, MemberRole.STAFF, encodedPassword, stats);
        return staffProfileRepository.findByMember_LoginId(loginId)
            .map(existing -> {
                stats.staffProfilesReused++;
                return existing;
            })
            .orElseGet(() -> {
                stats.staffProfilesCreated++;
                return staffProfileRepository.save(
                    StaffProfile.create(member, staffNo, department, position, responsibility)
                );
            });
    }

    private void createCourseTimeIfAbsent(
        CourseOffering offering,
        DayOfWeek dayOfWeek,
        int startPeriod,
        int endPeriod,
        SeedStats stats
    ) {
        boolean exists = courseTimeRepository
            .existsByCourseOfferingIdAndDayOfWeekAndStartPeriodAndEndPeriod(
                offering.getId(),
                dayOfWeek,
                startPeriod,
                endPeriod
            );
        if (exists) {
            stats.courseTimesReused++;
            return;
        }

        stats.courseTimesCreated++;
        courseTimeRepository.save(
            CourseTime.create(offering, dayOfWeek, startPeriod, endPeriod)
        );
    }

    private java.util.Optional<CoursePlan> findCoursePlan(CourseOffering offering) {
        return coursePlanRepository.findByCourseOfferingIdIn(List.of(offering.getId()))
            .stream()
            .findFirst();
    }

    private void createCoursePlanContentIfAbsent(
        CoursePlan plan,
        CoursePlanVersionType versionType,
        SeedStats stats
    ) {
        boolean exists = !coursePlanContentRepository
            .findByCoursePlanIdInAndVersionType(List.of(plan.getId()), versionType)
            .isEmpty();
        if (exists) {
            stats.coursePlanContentsReused++;
            return;
        }

        CoursePlanContent content = BeanUtils.instantiateClass(CoursePlanContent.class);
        setField(content, "coursePlan", plan);
        setField(content, "versionType", versionType);
        setField(content, "overview", "수강편람 검증용 강의계획서 개요입니다.");
        setField(content, "objective", "교과목의 핵심 개념과 응용 능력을 습득한다.");
        setField(content, "evaluationMethod", "중간고사 30%, 기말고사 40%, 과제 20%, 출석 10%");
        setField(content, "textbook", "담당 교수가 지정한 교재 및 강의자료");
        setField(content, "restrictionNote", "수강편람 검증용 더미데이터입니다.");
        stats.coursePlanContentsCreated++;
        coursePlanContentRepository.save(content);
    }

    private SubjectSeed subjectSeed(int index) {
        if (index < 100) {
            return new SubjectSeed("CSE%04d".formatted(1001 + index), subjectName(index));
        }
        if (index < 160) {
            return new SubjectSeed("ECE%04d".formatted(1001 + index - 100), subjectName(index));
        }
        if (index < 220) {
            return new SubjectSeed("MGT%04d".formatted(1001 + index - 160), subjectName(index));
        }
        if (index < 260) {
            return new SubjectSeed("ECO%04d".formatted(1001 + index - 220), subjectName(index));
        }
        if (index < 300) {
            return new SubjectSeed("KOR%04d".formatted(1001 + index - 260), subjectName(index));
        }
        if (index < 340) {
            return new SubjectSeed("ENG%04d".formatted(1001 + index - 300), subjectName(index));
        }
        if (index < 380) {
            return new SubjectSeed("MAT%04d".formatted(1001 + index - 340), subjectName(index));
        }
        if (index < 410) {
            return new SubjectSeed("PHY%04d".formatted(1001 + index - 380), subjectName(index));
        }
        if (index < 440) {
            return new SubjectSeed("CHM%04d".formatted(1001 + index - 410), subjectName(index));
        }
        return new SubjectSeed("GEN%04d".formatted(1001 + index - 440), subjectName(index));
    }

    private String subjectName(int index) {
        return SUBJECT_NAMES[index % SUBJECT_NAMES.length] + " " + ((index / SUBJECT_NAMES.length) + 1);
    }

    private String studentLoginId(int index) {
        return "stu%04d".formatted(index);
    }

    private SubjectCategory categoryFor(String code, int index) {
        if (code.startsWith("GEN")) {
            return index % 2 == 0
                ? SubjectCategory.GENERAL_REQUIRED
                : SubjectCategory.GENERAL_ELECTIVE;
        }
        return CATEGORIES[index % 3];
    }

    private CourseOfferingStatus statusFor(int index) {
        if (index % 20 == 0) {
            return CourseOfferingStatus.CLOSED;
        }
        if (index % 50 == 0) {
            return CourseOfferingStatus.CANCELLED;
        }
        return CourseOfferingStatus.OPEN;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                "Failed to set dummy data field: " + target.getClass().getSimpleName() + "." + fieldName,
                e
            );
        }
    }

    private record DepartmentSeed(String name, String collegeName, DepartmentType type) {
    }

    private record SubjectSeed(String code, String name) {
    }

    private static class SeedStats {

        private long departmentsCreated;
        private long departmentsReused;
        private long membersCreated;
        private long membersReused;
        private long studentProfilesCreated;
        private long studentProfilesReused;
        private long professorProfilesCreated;
        private long professorProfilesReused;
        private long staffProfilesCreated;
        private long staffProfilesReused;
        private long semestersCreated;
        private long semestersReused;
        private long subjectsCreated;
        private long subjectsReused;
        private long offeringsCreated;
        private long offeringsReused;
        private long courseTimesCreated;
        private long courseTimesReused;
        private long coursePlansCreated;
        private long coursePlansReused;
        private long coursePlanContentsCreated;
        private long coursePlanContentsReused;

        private long createdTotal() {
            return departmentsCreated + membersCreated + studentProfilesCreated
                + professorProfilesCreated + staffProfilesCreated + semestersCreated
                + subjectsCreated + offeringsCreated + courseTimesCreated
                + coursePlansCreated + coursePlanContentsCreated;
        }

        private long reusedTotal() {
            return departmentsReused + membersReused + studentProfilesReused
                + professorProfilesReused + staffProfilesReused + semestersReused
                + subjectsReused + offeringsReused + courseTimesReused
                + coursePlansReused + coursePlanContentsReused;
        }
    }
}
