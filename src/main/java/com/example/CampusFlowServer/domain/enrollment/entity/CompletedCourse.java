package com.example.CampusFlowServer.domain.enrollment.entity;

import com.example.CampusFlowServer.domain.semester.entity.Semester;
import com.example.CampusFlowServer.global.common.BaseEntity;
import com.example.CampusFlowServer.domain.course.entity.Subject;
import com.example.CampusFlowServer.domain.member.entity.StudentProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "completed_courses",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_completed_course_student_subject_semester",
            columnNames = {"student_profile_id", "subject_id", "completed_semester_id"}
        )
    },
    indexes = {
        @Index(
            name = "idx_completed_course_student",
            columnList = "student_profile_id"
        ),
        @Index(
            name = "idx_completed_course_student_subject",
            columnList = "student_profile_id, subject_id"
        )
    }
)
public class CompletedCourse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "completed_semester_id", nullable = false)
    private Semester completedSemester;

    @Column(name = "letter_grade", length = 10)
    private String letterGrade;

    @Column(nullable = false)
    private boolean passed = true;

    private LocalDateTime completedAt;

    private CompletedCourse(
        StudentProfile student,
        Subject subject,
        Semester completedSemester,
        String letterGrade,
        boolean passed,
        LocalDateTime completedAt
    ) {
        this.student = student;
        this.subject = subject;
        this.completedSemester = completedSemester;
        this.letterGrade = letterGrade;
        this.passed = passed;
        this.completedAt = completedAt;
    }

    public static CompletedCourse create(
        StudentProfile student,
        Subject subject,
        Semester completedSemester,
        String letterGrade,
        boolean passed,
        LocalDateTime completedAt
    ) {
        return new CompletedCourse(
            student,
            subject,
            completedSemester,
            letterGrade,
            passed,
            completedAt
        );
    }
}
