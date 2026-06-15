package com.example.CampusFlowServer.domain.semester.repository;

import com.example.CampusFlowServer.domain.semester.entity.SemesterSchedule;
import com.example.CampusFlowServer.domain.semester.enums.SemesterScheduleType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterScheduleRepository extends JpaRepository<SemesterSchedule, Long> {

    Optional<SemesterSchedule> findBySemesterIdAndType(Long semesterId, SemesterScheduleType type);
}
