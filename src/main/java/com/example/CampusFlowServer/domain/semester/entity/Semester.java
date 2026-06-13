package com.example.CampusFlowServer.domain.semester.entity;

import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import com.example.CampusFlowServer.global.common.BaseEntity;
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
    private Integer year;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SemesterTerm term;

    @Column(nullable = false, length = 50)
    private String name;
    @Column(name = "max_credit", nullable = false)
    private Integer maxCredit = 18;

    @Column(nullable = false)
    private boolean active = true;
}
