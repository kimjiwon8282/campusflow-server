package com.example.CampusFlowServer.domain.auth.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cookie")
public class CookieProperties {

	private String accessTokenName;
	private String refreshTokenName;
	private String csrfTokenName;
	private String csrfHeaderName;
	private boolean secure;
	private boolean httpOnly;
	private String sameSite;
	private Integer accessTokenMaxAgeSeconds;
	private Integer refreshTokenMaxAgeSeconds;
}
