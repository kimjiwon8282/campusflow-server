package com.example.CampusFlowServer.domain.member.repository;

import com.example.CampusFlowServer.domain.member.entity.Department;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByName(String name);
}
