package com.example.CampusFlowServer.global.config.properties;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

	private List<String> allowedOrigins;
	private List<String> allowedMethods;
	private List<String> allowedHeaders;
	private boolean allowCredentials;
}
