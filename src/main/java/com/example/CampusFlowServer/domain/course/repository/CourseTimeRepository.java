package com.example.CampusFlowServer.domain.course.repository;

import com.example.CampusFlowServer.domain.course.entity.CourseTime;
import com.example.CampusFlowServer.domain.course.enums.DayOfWeek;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseTimeRepository extends JpaRepository<CourseTime, Long> {

    List<CourseTime> findByCourseOfferingIdInOrderByCourseOfferingIdAscDayOfWeekAscStartPeriodAsc(
        Collection<Long> courseOfferingIds
    );

    boolean existsByCourseOfferingIdAndDayOfWeekAndStartPeriodAndEndPeriod(
        Long courseOfferingId,
        DayOfWeek dayOfWeek,
        Integer startPeriod,
        Integer endPeriod
    );
}
