package com.example.CampusFlowServer.domain.enrollment.entity;

import com.example.CampusFlowServer.domain.semester.entity.Semester;
import com.example.CampusFlowServer.global.common.BaseEntity;
import com.example.CampusFlowServer.domain.course.entity.CourseOffering;
import com.example.CampusFlowServer.domain.enrollment.enums.EnrollmentSource;
import com.example.CampusFlowServer.domain.enrollment.enums.EnrollmentStatus;
import com.example.CampusFlowServer.domain.member.entity.StudentProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    name = "enrollments",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_enrollment_student_course",
            columnNames = {"student_profile_id", "course_offering_id"}
        )
    },
    indexes = {
        @Index(
            name = "idx_enrollment_student_semester_status",
            columnList = "student_profile_id, semester_id, status"
        ),
        @Index(
            name = "idx_enrollment_course_status",
            columnList = "course_offering_id, status"
        ),
        @Index(
            name = "idx_enrollment_course_status_applied_id",
            columnList = "course_offering_id, status, applied_at, id"
        )
    }
)
public class Enrollment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EnrollmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EnrollmentSource source;

    @Column(name = "wait_number")
    private Integer waitNumber;

    @Column(nullable = false)
    private LocalDateTime appliedAt;

    private LocalDateTime cancelledAt;

    private Enrollment(
        StudentProfile student,
        Semester semester,
        CourseOffering courseOffering,
        EnrollmentStatus status,
        EnrollmentSource source,
        Integer waitNumber,
        LocalDateTime appliedAt,
        LocalDateTime cancelledAt
    ) {
        this.student = student;
        this.semester = semester;
        this.courseOffering = courseOffering;
        this.status = status;
        this.source = source;
        this.waitNumber = waitNumber;
        this.appliedAt = appliedAt == null ? LocalDateTime.now() : appliedAt;
        this.cancelledAt = cancelledAt;
    }

    public static Enrollment create(
        StudentProfile student,
        Semester semester,
        CourseOffering courseOffering,
        EnrollmentStatus status,
        EnrollmentSource source,
        Integer waitNumber,
        LocalDateTime appliedAt
    ) {
        return new Enrollment(
            student,
            semester,
            courseOffering,
            status,
            source,
            waitNumber,
            appliedAt,
            null
        );
    }

    public void reapply(
        EnrollmentStatus status,
        EnrollmentSource source,
        Integer waitNumber,
        LocalDateTime appliedAt
    ) {
        this.status = status;
        this.source = source;
        this.waitNumber = waitNumber;
        this.appliedAt = appliedAt == null ? LocalDateTime.now() : appliedAt;
        this.cancelledAt = null;
    }

    public void cancel(LocalDateTime cancelledAt) {
        this.status = EnrollmentStatus.CANCELLED;
        this.waitNumber = null;
        this.cancelledAt = cancelledAt == null ? LocalDateTime.now() : cancelledAt;
    }

    public void promoteToEnrolled(LocalDateTime appliedAt) {
        this.status = EnrollmentStatus.ENROLLED;
        this.waitNumber = null;
        this.appliedAt = appliedAt == null ? LocalDateTime.now() : appliedAt;
        this.cancelledAt = null;
    }
}
