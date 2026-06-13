package com.example.CampusFlowServer.domain.courseplan.entity;

import com.example.CampusFlowServer.domain.courseplan.enums.CoursePlanVersionType;
import com.example.CampusFlowServer.global.common.BaseEntity;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "course_plan_contents",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_course_plan_content_version",
            columnNames = {"course_plan_id", "version_type"}
        )
    }
)
public class CoursePlanContent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_plan_id", nullable = false)
    private CoursePlan coursePlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "version_type", nullable = false, length = 30)
    private CoursePlanVersionType versionType;

    @Column(length = 1000)
    private String overview;

    @Column(length = 1000)
    private String objective;

    @Column(name = "evaluation_method", length = 1000)
    private String evaluationMethod;

    @Column(length = 500)
    private String textbook;

    @Column(name = "restriction_note", length = 500)
    private String restrictionNote;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;
}
