package com.example.CampusFlowServer.domain.enrollment.entity;

import com.example.CampusFlowServer.domain.academic.entity.Semester;
import com.example.CampusFlowServer.domain.common.BaseEntity;
import com.example.CampusFlowServer.domain.course.entity.CourseOffering;
import com.example.CampusFlowServer.domain.enrollment.enums.WishAutoApplyResult;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "wish_courses",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_wish_course_student_course",
            columnNames = {"student_profile_id", "course_offering_id"}
        )//동일 학생이 동일 개설 회망과목으로 중복 담는 상황 방지
    },
    indexes = {
        @Index(
            name = "idx_wish_course_student_semester",
            columnList = "student_profile_id, semester_id"
        ),
        @Index(
            name = "idx_wish_course_course",
            columnList = "course_offering_id"
        )
    }
)
public class WishCourse extends BaseEntity {

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

    @Column(nullable = false)
    private boolean autoApply = false; //자동 신청 선택 여부

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WishAutoApplyResult result; //자동 신청 결과 상태

    @Column(length = 255)
    private String resultMessage; //자동 신청 결과 메시지
}
