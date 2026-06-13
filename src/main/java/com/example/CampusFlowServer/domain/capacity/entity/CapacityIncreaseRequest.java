package com.example.CampusFlowServer.domain.capacity.entity;

import com.example.CampusFlowServer.global.common.BaseEntity;
import com.example.CampusFlowServer.domain.course.entity.CourseOffering;
import com.example.CampusFlowServer.domain.member.entity.ProfessorProfile;
import com.example.CampusFlowServer.domain.member.entity.StaffProfile;
import com.example.CampusFlowServer.domain.capacity.enums.CapacityIncreaseRequestStatus;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "capacity_increase_requests",
    indexes = {
        @Index(name = "idx_capacity_request_course_status", columnList = "course_offering_id, status"),
        @Index(name = "idx_capacity_request_professor_status", columnList = "professor_profile_id, status"),
        @Index(name = "idx_capacity_request_processed_by", columnList = "processed_by")
    }
)
public class CapacityIncreaseRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_profile_id", nullable = false)
    private ProfessorProfile professor;

    @Column(name = "current_capacity", nullable = false)
    private Integer currentCapacity;

    @Column(name = "requested_capacity", nullable = false)
    private Integer requestedCapacity;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CapacityIncreaseRequestStatus status = CapacityIncreaseRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private StaffProfile processedBy;
    private LocalDateTime processedAt;

    @Column(name = "processed_comment", length = 1000)
    private String processedComment;
}
