package com.example.CampusFlowServer;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.CampusFlowServer.domain.student.enrollment.scheduler.AutoEnrollmentPreApplyScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CampusFlowServerApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
	}

	@Test
	void autoEnrollmentSchedulerIsDisabledInTestProfile() {
		assertThat(applicationContext.getBeansOfType(AutoEnrollmentPreApplyScheduler.class))
			.isEmpty();
	}

}
