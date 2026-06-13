package com.example.CampusFlowServer.domain.auth.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.refresh-token-cleanup")
public class RefreshTokenCleanupProperties {

    private boolean enabled;
    private String cron;
    private long revokedRetentionDays;
}
