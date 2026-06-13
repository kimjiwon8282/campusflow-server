package com.example.CampusFlowServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class CampusFlowServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CampusFlowServerApplication.class, args);
	}

}
