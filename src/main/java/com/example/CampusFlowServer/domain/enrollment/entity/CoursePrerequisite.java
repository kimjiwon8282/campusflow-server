package com.example.CampusFlowServer.domain.enrollment.entity;

import com.example.CampusFlowServer.global.common.BaseEntity;
import com.example.CampusFlowServer.domain.course.entity.Subject;
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
    name = "course_prerequisites",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_course_prerequisite",
            columnNames = {"subject_id", "prerequisite_subject_id"}
        )
    },
    indexes = {
        @Index(
            name = "idx_course_prerequisite_subject",
            columnList = "subject_id"
        )
    }
)
public class CoursePrerequisite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prerequisite_subject_id", nullable = false)
    private Subject prerequisiteSubject;

    @Column(nullable = false)
    private boolean active = true;
}
