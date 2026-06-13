package com.example.CampusFlowServer.domain.course.entity;

import com.example.CampusFlowServer.domain.academic.entity.Semester;
import com.example.CampusFlowServer.domain.common.BaseEntity;
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
    private StudentProfile student; //이수 학생

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject; // 이수 과목

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "completed_semester_id", nullable = false)
    private Semester completedSemester; //이수 학기

    @Column(name = "letter_grade", length = 10)
    private String letterGrade; //성적 표기

    @Column(nullable = false)
    private boolean passed = true; //이수 인정 여부

    private LocalDateTime completedAt;
}
