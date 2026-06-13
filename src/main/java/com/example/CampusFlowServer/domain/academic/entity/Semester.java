package com.example.CampusFlowServer.domain.academic.entity;

import com.example.CampusFlowServer.domain.academic.enums.SemesterTerm;
import com.example.CampusFlowServer.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "semesters",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_semester_year_term", columnNames = {"year", "term"})
    }
)
public class Semester extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year; //학년도

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SemesterTerm term; //학기 구분

    @Column(nullable = false, length = 50)
    private String name; //학기명

    @Column(name = "max_credit", nullable = false)
    private Integer maxCredit = 18; //기본 최대 신청 학점

    @Column(nullable = false)
    private boolean active = true;
}
