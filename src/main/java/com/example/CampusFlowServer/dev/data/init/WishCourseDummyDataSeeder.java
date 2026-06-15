package com.example.CampusFlowServer.dev.data.init;

import com.example.CampusFlowServer.domain.course.entity.CourseOffering;
import com.example.CampusFlowServer.domain.course.repository.CourseOfferingRepository;
import com.example.CampusFlowServer.domain.enrollment.entity.WishCourse;
import com.example.CampusFlowServer.domain.enrollment.enums.WishAutoApplyResult;
import com.example.CampusFlowServer.domain.enrollment.repository.WishCourseRepository;
import com.example.CampusFlowServer.domain.member.entity.StudentProfile;
import com.example.CampusFlowServer.domain.member.repository.StudentProfileRepository;
import com.example.CampusFlowServer.domain.semester.entity.Semester;
import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import com.example.CampusFlowServer.domain.semester.repository.SemesterRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("wish-seed")
@RequiredArgsConstructor
public class WishCourseDummyDataSeeder implements ApplicationRunner {

    private static final int TARGET_YEAR = 2026;
    private static final SemesterTerm TARGET_TERM = SemesterTerm.FIRST;
    private static final int STUDENT_CHUNK_SIZE = 1000;
    private static final int SAVE_CHUNK_SIZE = 1000;
    private static final int MIN_WISH_COUNT_PER_STUDENT = 4;
    private static final int MAX_WISH_COUNT_PER_STUDENT = 6;
    private static final int STUDENT_OFFSET_STEP = 7;

    private final SemesterRepository semesterRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final WishCourseRepository wishCourseRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Semester semester = semesterRepository.findByYearAndTerm(TARGET_YEAR, TARGET_TERM)
            .orElseThrow(() -> new IllegalStateException("2026 FIRST semester not found. Run catalog-seed first."));

        List<CourseOffering> courseOfferings = courseOfferingRepository.findBySemesterId(semester.getId());
        if (courseOfferings.isEmpty()) {
            log.warn("WishCourse seed skipped. No course offerings found for semesterId={}", semester.getId());
            return;
        }

        List<StudentProfile> students = studentProfileRepository.findAll();
        if (students.isEmpty()) {
            log.warn("WishCourse seed skipped. No student profiles found.");
            return;
        }

        long createdCount = 0L;
        long reusedCount = 0L;
        long skippedStudentCount = 0L;
        List<WishCourse> pendingWishes = new ArrayList<>(SAVE_CHUNK_SIZE);

        log.info(
            "WishCourse seed started. semesterId={}, students={}, courseOfferings={}",
            semester.getId(),
            students.size(),
            courseOfferings.size()
        );

        for (int start = 0; start < students.size(); start += STUDENT_CHUNK_SIZE) {
            int end = Math.min(start + STUDENT_CHUNK_SIZE, students.size());
            List<StudentProfile> studentChunk = students.subList(start, end);
            Map<Long, Set<Long>> existingOfferingIdsByStudentId = loadExistingOfferingIds(
                studentChunk,
                semester.getId()
            );

            for (int i = 0; i < studentChunk.size(); i++) {
                StudentProfile student = studentChunk.get(i);
                int studentIndex = start + i;
                Set<Long> existingOfferingIds = existingOfferingIdsByStudentId.getOrDefault(
                    student.getId(),
                    new HashSet<>()
                );
                int existingWishCount = existingOfferingIds.size();
                int targetWishCount = targetWishCount(studentIndex);
                int maxCredit = maxCreditOf(student, semester);
                int selectedCredit = existingCredit(existingOfferingIds, courseOfferings);
                int createdForStudent = 0;
                Set<Long> selectedOfferingIds = new HashSet<>(existingOfferingIds);

                reusedCount += existingWishCount;

                for (CourseOffering courseOffering : orderedOfferingsForStudent(
                    student,
                    studentIndex,
                    courseOfferings
                )) {
                    if (existingWishCount + createdForStudent >= targetWishCount) {
                        break;
                    }
                    if (selectedOfferingIds.contains(courseOffering.getId())) {
                        continue;
                    }

                    int credit = creditOf(courseOffering);
                    if (credit <= 0 || selectedCredit + credit > maxCredit) {
                        continue;
                    }

                    pendingWishes.add(WishCourse.create(
                        student,
                        semester,
                        courseOffering,
                        false,
                        WishAutoApplyResult.NOT_SELECTED,
                        null
                    ));
                    selectedOfferingIds.add(courseOffering.getId());
                    selectedCredit += credit;
                    createdForStudent++;

                    if (pendingWishes.size() >= SAVE_CHUNK_SIZE) {
                        createdCount += savePending(pendingWishes);
                    }
                }

                if (existingWishCount + createdForStudent == 0) {
                    skippedStudentCount++;
                }
            }

            createdCount += savePending(pendingWishes);
            entityManager.flush();
            entityManager.clear();
            log.info(
                "WishCourse seed chunk processed. students={}..{}, created={}, reused={}, skippedStudents={}",
                start + 1,
                end,
                createdCount,
                reusedCount,
                skippedStudentCount
            );
        }

        log.info(
            "WishCourse seed completed. semesterId={}, created={}, reused={}, skippedStudents={}",
            semester.getId(),
            createdCount,
            reusedCount,
            skippedStudentCount
        );
    }

    private Map<Long, Set<Long>> loadExistingOfferingIds(
        List<StudentProfile> students,
        Long semesterId
    ) {
        List<Long> studentIds = students.stream()
            .map(StudentProfile::getId)
            .toList();
        List<WishCourse> existingWishes = wishCourseRepository.findByStudentIdInAndSemesterId(
            studentIds,
            semesterId
        );

        Map<Long, Set<Long>> offeringIdsByStudentId = new HashMap<>();
        for (WishCourse wishCourse : existingWishes) {
            offeringIdsByStudentId
                .computeIfAbsent(wishCourse.getStudent().getId(), ignored -> new HashSet<>())
                .add(wishCourse.getCourseOffering().getId());
        }
        return offeringIdsByStudentId;
    }

    private List<CourseOffering> orderedOfferingsForStudent(
        StudentProfile student,
        int studentIndex,
        List<CourseOffering> courseOfferings
    ) {
        int size = courseOfferings.size();
        int offset = Math.floorMod(
            studentIndex * STUDENT_OFFSET_STEP + Long.hashCode(student.getId()),
            size
        );
        List<CourseOffering> ordered = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ordered.add(courseOfferings.get((offset + i) % size));
        }
        return ordered;
    }

    private int targetWishCount(int studentIndex) {
        return MIN_WISH_COUNT_PER_STUDENT
            + Math.floorMod(studentIndex, MAX_WISH_COUNT_PER_STUDENT - MIN_WISH_COUNT_PER_STUDENT + 1);
    }

    private int maxCreditOf(StudentProfile student, Semester semester) {
        if (student.getMaxCredit() != null && student.getMaxCredit() > 0) {
            return student.getMaxCredit();
        }
        return semester.getMaxCredit();
    }

    private int existingCredit(Set<Long> existingOfferingIds, List<CourseOffering> courseOfferings) {
        if (existingOfferingIds.isEmpty()) {
            return 0;
        }

        int creditSum = 0;
        for (CourseOffering courseOffering : courseOfferings) {
            if (existingOfferingIds.contains(courseOffering.getId())) {
                creditSum += creditOf(courseOffering);
            }
        }
        return creditSum;
    }

    private int creditOf(CourseOffering courseOffering) {
        Integer credit = courseOffering.getSubject().getCredit();
        return credit == null ? 0 : credit;
    }

    private long savePending(List<WishCourse> pendingWishes) {
        if (pendingWishes.isEmpty()) {
            return 0L;
        }

        int size = pendingWishes.size();
        wishCourseRepository.saveAll(pendingWishes);
        pendingWishes.clear();
        return size;
    }
}
