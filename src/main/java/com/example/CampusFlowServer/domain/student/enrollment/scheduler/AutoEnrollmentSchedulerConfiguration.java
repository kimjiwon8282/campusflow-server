package com.example.CampusFlowServer.domain.student.enrollment.scheduler;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AutoEnrollmentSchedulerConfiguration {

    @Bean("autoEnrollmentSchedulerClock")
    public Clock autoEnrollmentSchedulerClock(
        @Value("${campusflow.auto-enrollment.pre-apply.scheduler.zone:Asia/Seoul}") String zone
    ) {
        return Clock.system(ZoneId.of(zone));
    }
}
