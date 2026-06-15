package com.example.CampusFlowServer.domain.semester.repository;

import com.example.CampusFlowServer.domain.semester.entity.Semester;
import com.example.CampusFlowServer.domain.semester.enums.SemesterTerm;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    Optional<Semester> findByYearAndTerm(Integer year, SemesterTerm term);
}
