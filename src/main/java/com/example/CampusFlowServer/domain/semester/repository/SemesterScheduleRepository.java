package com.example.CampusFlowServer.domain.semester.repository;

import com.example.CampusFlowServer.domain.semester.entity.SemesterSchedule;
import com.example.CampusFlowServer.domain.semester.enums.SemesterScheduleType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SemesterScheduleRepository extends JpaRepository<SemesterSchedule, Long> {

    Optional<SemesterSchedule> findBySemesterIdAndType(Long semesterId, SemesterScheduleType type);

    @EntityGraph(attributePaths = "semester")
    @Query("""
        select schedule
        from SemesterSchedule schedule
        where schedule.type = :type
          and schedule.alwaysOpen = false
          and schedule.startAt is not null
          and schedule.startAt >= :from
          and schedule.startAt < :to
        order by schedule.id asc
        """)
    List<SemesterSchedule> findPreApplyTargets(
        @Param("type") SemesterScheduleType type,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}
