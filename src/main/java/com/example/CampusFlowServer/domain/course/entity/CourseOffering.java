package com.example.CampusFlowServer.domain.course.entity;

import com.example.CampusFlowServer.domain.course.enums.CourseOfferingStatus;
import com.example.CampusFlowServer.domain.member.entity.ProfessorProfile;
import com.example.CampusFlowServer.domain.member.entity.StaffProfile;
import com.example.CampusFlowServer.domain.semester.entity.Semester;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "course_offerings")
public class CourseOffering extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_profile_id", nullable = false)
    private ProfessorProfile professor;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseOfferingStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private StaffProfile createdBy;

    private CourseOffering(
        Semester semester,
        Subject subject,
        ProfessorProfile professor,
        Integer capacity,
        CourseOfferingStatus status,
        StaffProfile createdBy
    ) {
        this.semester = semester;
        this.subject = subject;
        this.professor = professor;
        this.capacity = capacity;
        this.status = status;
        this.createdBy = createdBy;
    }

    public static CourseOffering create(
        Semester semester,
        Subject subject,
        ProfessorProfile professor,
        Integer capacity,
        CourseOfferingStatus status,
        StaffProfile createdBy
    ) {
        return new CourseOffering(semester, subject, professor, capacity, status, createdBy);
    }

    public void updateForDummyData(
        ProfessorProfile professor,
        Integer capacity,
        CourseOfferingStatus status,
        StaffProfile createdBy
    ) {
        this.professor = professor;
        this.capacity = capacity;
        this.status = status;
        this.createdBy = createdBy;
    }
}
