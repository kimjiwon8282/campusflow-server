package com.example.CampusFlowServer.domain.courseplan.entity;

import com.example.CampusFlowServer.global.common.BaseEntity;
import com.example.CampusFlowServer.domain.course.entity.CourseOffering;
import com.example.CampusFlowServer.domain.courseplan.enums.CoursePlanStatus;
import com.example.CampusFlowServer.domain.member.entity.ProfessorProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
    name = "course_plans",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_course_plan_course_offering",
            columnNames = "course_offering_id"
        )
    }
)
public class CoursePlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CoursePlanStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private ProfessorProfile createdBy;

    private LocalDateTime publishedAt;
}
