package com.example.CampusFlowServer.domain.enrollment.entity;

import com.example.CampusFlowServer.global.common.BaseEntity;
import com.example.CampusFlowServer.domain.course.entity.CourseOffering;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "course_allowed_grades",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_course_allowed_grade",
            columnNames = {"course_offering_id", "grade_level"}
        )
    },
    indexes = {
        @Index(
            name = "idx_course_allowed_grade_course",
            columnList = "course_offering_id"
        )
    }
)
public class CourseAllowedGrade extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering;

    @Column(name = "grade_level", nullable = false)
    private Integer gradeLevel;

    private CourseAllowedGrade(CourseOffering courseOffering, Integer gradeLevel) {
        this.courseOffering = courseOffering;
        this.gradeLevel = gradeLevel;
    }

    public static CourseAllowedGrade create(CourseOffering courseOffering, Integer gradeLevel) {
        return new CourseAllowedGrade(courseOffering, gradeLevel);
    }
}
