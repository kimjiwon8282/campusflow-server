package com.example.CampusFlowServer.domain.student.enrollment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.CampusFlowServer.domain.course.entity.Subject;
import com.example.CampusFlowServer.domain.course.enums.SubjectCategory;
import com.example.CampusFlowServer.domain.course.repository.SubjectRepository;
import com.example.CampusFlowServer.domain.enrollment.enums.EnrollmentStatus;
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
import com.example.CampusFlowServer.domain.semester.enums.SemesterScheduleType;
import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import com.example.CampusFlowServer.domain.semester.repository.SemesterRepository;
import com.example.CampusFlowServer.domain.semester.repository.SemesterScheduleRepository;
import com.example.CampusFlowServer.domain.student.enrollment.dto.StudentEnrollmentApplyRequest;
import com.example.CampusFlowServer.domain.student.enrollment.service.StudentEnrollmentService;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class StudentEnrollmentConcurrencyIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(
        StudentEnrollmentConcurrencyIntegrationTest.class
    );

    private static final long COURSE_OFFERING_ID = 4L; //테스트 대상 강의
    private static final int CAPACITY = 1; //테스트 대상 정원
    private static final int THREAD_COUNT = 20; //동시에 수강신청을 시도할 학생 수 20
    private static final int ROUND_COUNT = 10; //동시성 문제가 매번 재현되지 않을 수 있으므로 최대 10회 반복

    @Autowired
    private StudentEnrollmentService studentEnrollmentService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MemberRepository memberRepository;

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

    @AfterEach
    void cleanUp() {
        resetEnrollments(COURSE_OFFERING_ID);
    }

    @Test
    @Disabled("비관적 락 적용 전 동시성 문제 재현용 테스트")
    void concurrentApplicationsCanCreateMoreEnrolledThanCapacity() throws Exception {
        TestFixture fixture = prepareFixture(); //테스트 시작 시 Fixture생성
        boolean reproduced = false;
        RoundResult lastResult = null;

        for (int round = 1; round <= ROUND_COUNT; round++) { //최대 10라운드 반복
            resetEnrollments(COURSE_OFFERING_ID); //기존 이력 삭제

            RoundResult result = runRound(round, fixture.memberIds());
            lastResult = result;
            logRound(result);

            if (result.enrolledCount() > CAPACITY) {
                reproduced = true;
                break;
            }
        }

        assertThat(reproduced)
            .as("Expected at least one round to exceed capacity=%s. Last result=%s",
                CAPACITY,
                lastResult)
            .isTrue();
    }

    @Test
    void concurrentApplicationsDoNotExceedCapacityWithPessimisticLock() throws Exception {
        TestFixture fixture = prepareFixture();

        for (int round = 1; round <= ROUND_COUNT; round++) {
            resetEnrollments(COURSE_OFFERING_ID);

            RoundResult result = runRound(round, fixture.memberIds());
            logRound(result);

            assertThat(result.enrolledCount()).isEqualTo(CAPACITY);
            assertThat(result.waitingCount()).isEqualTo(THREAD_COUNT - CAPACITY);
            assertThat(result.exceptions()).isEmpty();
        }
    }

    private TestFixture prepareFixture() { //테스트 학기 준비
        return transactionTemplate.execute(status -> {
            Semester semester = semesterRepository.findByYearAndTerm(2026, SemesterTerm.FIRST)
                .orElseGet(() -> semesterRepository.saveAndFlush(
                    Semester.create(2026, SemesterTerm.FIRST, "2026 FIRST", 18)
                ));
            ensureEnrollmentScheduleOpen(semester); //수강신청 기간 open처리

            Department department = departmentRepository.findByName("Concurrency Test Department")
                .orElseGet(() -> departmentRepository.saveAndFlush(
                    Department.create(
                        "Concurrency Test Department",
                        "Engineering",
                        DepartmentType.DEPARTMENT
                    )
                ));//테스트 전용 학과
            ProfessorProfile professor = ensureProfessor(department); //테스트 전용 교수
            StaffProfile staff = ensureStaff(department); //테스트 전용 교직원
            Subject subject = subjectRepository.findByCode("CONC101")
                .orElseGet(() -> subjectRepository.saveAndFlush(
                    Subject.create(
                        "CONC101",
                        "Concurrency Test Course",
                        department,
                        3,
                        SubjectCategory.MAJOR_REQUIRED
                    )
                ));

            ensureCourseOffering4(semester, professor, staff, subject); //강의를 테스트 조건에 맞게 조정
            List<Member> students = ensureStudents(department); //테스트 학생 20면 준비
            entityManager.flush();
            entityManager.clear();

            return new TestFixture(students.stream().map(Member::getId).toList());
        });
    }

    private void ensureEnrollmentScheduleOpen(Semester semester) {
        LocalDateTime now = LocalDateTime.now();
        semesterScheduleRepository.findBySemesterIdAndType(
                semester.getId(),
                SemesterScheduleType.ENROLLMENT
            )
            .ifPresentOrElse(
                schedule -> schedule.updatePeriod(now.minusDays(1), now.plusDays(1), null),
                () -> semesterScheduleRepository.saveAndFlush(SemesterSchedule.create(
                    semester,
                    SemesterScheduleType.ENROLLMENT,
                    now.minusDays(1),
                    now.plusDays(1),
                    null,
                    false
                ))
            );
    }

    private ProfessorProfile ensureProfessor(Department department) {
        return professorProfileRepository.findByMember_LoginId("conc-professor")
            .orElseGet(() -> {
                Member member = memberRepository.saveAndFlush(Member.create(
                    "conc-professor",
                    "password123!",
                    "Concurrency Professor",
                    MemberRole.PROFESSOR
                ));
                return professorProfileRepository.saveAndFlush(
                    ProfessorProfile.create(member, "CONC-PROF-001", department, "Professor")
                );
            });
    }

    private StaffProfile ensureStaff(Department department) {
        return staffProfileRepository.findByMember_LoginId("conc-staff")
            .orElseGet(() -> {
                Member member = memberRepository.saveAndFlush(Member.create(
                    "conc-staff",
                    "password123!",
                    "Concurrency Staff",
                    MemberRole.STAFF
                ));
                return staffProfileRepository.saveAndFlush(
                    StaffProfile.create(member, "CONC-STAFF-001", department, "Staff", "Courses")
                );
            });
    }

    private void ensureCourseOffering4(
        Semester semester,
        ProfessorProfile professor,
        StaffProfile staff,
        Subject subject
    ) {
        Number existingCount = (Number) entityManager
            .createNativeQuery("select count(*) from course_offerings where id = :id")
            .setParameter("id", COURSE_OFFERING_ID)
            .getSingleResult();

        if (existingCount.longValue() == 0L) {
            entityManager.createNativeQuery("""
                    insert into course_offerings
                        (id, semester_id, subject_id, professor_profile_id, capacity, status,
                         created_by, created_at, updated_at)
                    values
                        (:id, :semesterId, :subjectId, :professorId, :capacity, 'OPEN',
                         :staffId, current_timestamp, current_timestamp)
                    """)
                .setParameter("id", COURSE_OFFERING_ID)
                .setParameter("semesterId", semester.getId())
                .setParameter("subjectId", subject.getId())
                .setParameter("professorId", professor.getId())
                .setParameter("capacity", CAPACITY)
                .setParameter("staffId", staff.getId())
                .executeUpdate();
            return;
        }

        entityManager.createNativeQuery("""
                update course_offerings
                set semester_id = :semesterId,
                    subject_id = :subjectId,
                    professor_profile_id = :professorId,
                    capacity = :capacity,
                    status = 'OPEN',
                    created_by = :staffId,
                    updated_at = current_timestamp
                where id = :id
                """)
            .setParameter("semesterId", semester.getId())
            .setParameter("subjectId", subject.getId())
            .setParameter("professorId", professor.getId())
            .setParameter("capacity", CAPACITY)
            .setParameter("staffId", staff.getId())
            .setParameter("id", COURSE_OFFERING_ID)
            .executeUpdate();
    }

    private List<Member> ensureStudents(Department department) {
        List<String> loginIds = IntStream.range(100, 100 + THREAD_COUNT)
            .mapToObj(number -> "stu%04d".formatted(number))
            .toList();
        Map<String, StudentProfile> existingProfiles = studentProfileRepository
            .findByMember_LoginIdIn(loginIds)
            .stream()
            .collect(Collectors.toMap(
                profile -> profile.getMember().getLoginId(),
                Function.identity()
            ));

        List<Member> members = new ArrayList<>();
        for (String loginId : loginIds) {
            StudentProfile profile = existingProfiles.get(loginId);
            if (profile == null) {
                Member member = memberRepository.saveAndFlush(Member.create(
                    loginId,
                    "password123!",
                    "Student " + loginId,
                    MemberRole.STUDENT
                ));
                profile = studentProfileRepository.saveAndFlush(StudentProfile.create(
                    member,
                    "SN-" + loginId,
                    department,
                    2,
                    AcademicStatus.ENROLLED,
                    18
                ));
            } else {
                profile.changeMaxCredit(18);
            }
            members.add(profile.getMember());
        }
        return members;
    }

    private void resetEnrollments(long courseOfferingId) { //테스트 대상 강의 신청 이력 삭제
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createNativeQuery(
                    "delete from enrollments where course_offering_id = :courseOfferingId"
                )
                .setParameter("courseOfferingId", courseOfferingId)
                .executeUpdate();
            entityManager.flush();
            entityManager.clear();
        });
    }

    private RoundResult runRound(int round, List<Long> memberIds) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT); //20개 작업을 동시에 실행하기 위한 스레드풀
        CountDownLatch readyGate = new CountDownLatch(THREAD_COUNT);//스레드 시작 시점을 맞추기 위한 동기화 도구(준비상태에 도달 확인)
        CountDownLatch startGate = new CountDownLatch(1); //동시에 출발시키기 위한 용도
        CountDownLatch doneGate = new CountDownLatch(THREAD_COUNT); //모든 작업 스레드가 종료될때까지 기다리기 위한 용도
        Queue<Throwable> exceptions = new ConcurrentLinkedQueue<>();

        try {
            for (Long memberId : memberIds) {
                executorService.submit(() -> {
                    readyGate.countDown(); //준비 상태에 도달했는지 확인하는 용도
                    try {
                        startGate.await();
                        studentEnrollmentService.apply( //실제 작업 수행(같은 수강신청 요청)
                            memberId,
                            new StudentEnrollmentApplyRequest(COURSE_OFFERING_ID)
                        );
                    } catch (Throwable throwable) {
                        exceptions.add(throwable);
                    } finally {
                        doneGate.countDown();
                    }
                });
            }

            assertThat(readyGate.await(10, TimeUnit.SECONDS))
                .as("All worker threads should be ready before start")
                .isTrue();
            startGate.countDown();
            assertThat(doneGate.await(30, TimeUnit.SECONDS))
                .as("All worker threads should finish")
                .isTrue();
        } finally {
            executorService.shutdownNow();
        }

        StatusCounts counts = countStatuses(COURSE_OFFERING_ID);
        return new RoundResult(
            round,
            THREAD_COUNT,
            CAPACITY,
            counts.enrolledCount(),
            counts.waitingCount(),
            List.copyOf(exceptions)
        );
    }

    private StatusCounts countStatuses(long courseOfferingId) {
        return transactionTemplate.execute(status -> {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = entityManager.createNativeQuery("""
                    select status, count(*)
                    from enrollments
                    where course_offering_id = :courseOfferingId
                      and status in ('ENROLLED', 'WAITING')
                    group by status
                    """)
                .setParameter("courseOfferingId", courseOfferingId)
                .getResultList();

            Map<String, Long> counts = rows.stream()
                .collect(Collectors.toMap(
                    row -> Objects.toString(row[0]),
                    row -> ((Number) row[1]).longValue()
                ));
            return new StatusCounts(
                counts.getOrDefault(EnrollmentStatus.ENROLLED.name(), 0L),
                counts.getOrDefault(EnrollmentStatus.WAITING.name(), 0L)
            );
        });
    }

    private void logRound(RoundResult result) {
        log.info(
            "round = {}, threadCount = {}, capacity = {}, ENROLLED count = {}, WAITING count = {}, exception count = {}",
            result.round(),
            result.threadCount(),
            result.capacity(),
            result.enrolledCount(),
            result.waitingCount(),
            result.exceptions().size()
        );

        if (!result.exceptions().isEmpty()) {
            Map<String, Long> exceptionCounts = result.exceptions()
                .stream()
                .map(throwable -> throwable.getClass().getName() + ": " + throwable.getMessage())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            exceptionCounts.forEach((message, count) ->
                log.info("round = {}, exception = {}, count = {}", result.round(), message, count)
            );
        }
    }

    private record TestFixture(List<Long> memberIds) {
    }

    private record StatusCounts(long enrolledCount, long waitingCount) {
    }

    private record RoundResult(
        int round,
        int threadCount,
        int capacity,
        long enrolledCount,
        long waitingCount,
        Collection<Throwable> exceptions
    ) {
    }
}
