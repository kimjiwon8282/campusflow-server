package com.example.CampusFlowServer.domain.member.entity;

import com.example.CampusFlowServer.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "professor_profiles",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_professor_profile_member", columnNames = "member_id"),
        @UniqueConstraint(name = "uk_professor_no", columnNames = "professor_no")
    }
)
public class ProfessorProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "professor_no", nullable = false, unique = true, length = 30)
    private String professorNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(length = 50)
    private String position;

    @Column(nullable = false)
    private boolean active = true;

    private ProfessorProfile(Member member, String professorNo, Department department, String position) {
        this.member = member;
        this.professorNo = professorNo;
        this.department = department;
        this.position = position;
    }

    public static ProfessorProfile create(
        Member member,
        String professorNo,
        Department department,
        String position
    ) {
        return new ProfessorProfile(member, professorNo, department, position);
    }

    public void changeDepartment(Department department) {
        this.department = department;
    }
}
