package com.example.CampusFlowServer.domain.member.entity;

import com.example.CampusFlowServer.global.common.BaseEntity;
import com.example.CampusFlowServer.domain.member.enums.DepartmentType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "departments")
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String collegeName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DepartmentType type;

    @Column(nullable = false)
    private boolean active = true;

    private Department(String name, String collegeName, DepartmentType type) {
        this.name = name;
        this.collegeName = collegeName;
        this.type = type;
    }

    public static Department create(String name, String collegeName, DepartmentType type) {
        return new Department(name, collegeName, type);
    }
}
