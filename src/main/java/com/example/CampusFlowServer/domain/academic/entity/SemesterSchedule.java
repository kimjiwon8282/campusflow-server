package com.example.CampusFlowServer.domain.academic.entity;

import com.example.CampusFlowServer.domain.academic.enums.SemesterScheduleType;
import com.example.CampusFlowServer.domain.common.BaseEntity;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "semester_schedules",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_semester_schedule_type", columnNames = {"semester_id", "type"})
    }
)
public class SemesterSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester; //대상 학기

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SemesterScheduleType type; //일정 유형

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private LocalDateTime openAt; //공개 일시

    @Column(nullable = false)
    private boolean alwaysOpen = false; //상시 공개 여부
}
