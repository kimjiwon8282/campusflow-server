package com.example.CampusFlowServer.domain.member.entity;

import com.example.CampusFlowServer.global.common.BaseEntity;
import com.example.CampusFlowServer.domain.member.enums.DepartmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
}
