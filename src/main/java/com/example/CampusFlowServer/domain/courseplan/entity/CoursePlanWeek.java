package com.example.CampusFlowServer.domain.courseplan.entity;

import com.example.CampusFlowServer.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "course_plan_weeks",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_course_plan_week",
            columnNames = {"course_plan_content_id", "week_no"}
        )
    }
)
public class CoursePlanWeek extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_plan_content_id", nullable = false)
    private CoursePlanContent coursePlanContent;// 대상 강의 계획서 내용

    @Column(name = "week_no", nullable = false)
    private Integer weekNo; //주차 번호

    @Column(length = 1000)
    private String content; //주차별 수업 내용
}
