package com.example.CampusFlowServer.domain.auth.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

	private String accessSecret;
	private String refreshSecret;
	private Long accessTokenValidityMs;
	private Long refreshTokenValidityMs;
}
