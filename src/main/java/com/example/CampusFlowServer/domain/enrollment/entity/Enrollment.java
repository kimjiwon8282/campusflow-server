package com.example.CampusFlowServer.domain.enrollment.entity;

import com.example.CampusFlowServer.domain.academic.entity.Semester;
import com.example.CampusFlowServer.domain.common.BaseEntity;
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
        )// 동일 학생이 동일 개설 강의에 중복 신청 방지
    },
    indexes = {
        @Index(
            name = "idx_enrollment_student_semester_status",
            columnList = "student_profile_id, semester_id, status"
        ),
        @Index(
            name = "idx_enrollment_course_status",
            columnList = "course_offering_id, status"
        )
    }
)
public class Enrollment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile student; //신청 학생

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester; //신청 학기

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering; //신청 대상 개설 강의

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EnrollmentStatus status; //수강 신청 상태

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EnrollmentSource source; //신청 경로

    @Column(name = "wait_number")
    private Integer waitNumber; //대기 순번

    @Column(nullable = false)
    private LocalDateTime appliedAt; //신청 시각

    private LocalDateTime cancelledAt; //취소 시각
}
