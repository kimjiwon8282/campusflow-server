package com.example.CampusFlowServer.domain.course.repository;

import com.example.CampusFlowServer.domain.course.entity.Subject;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByCode(String code);
}
