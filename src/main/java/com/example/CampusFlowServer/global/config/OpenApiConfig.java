package com.example.CampusFlowServer.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String ACCESS_TOKEN_COOKIE_AUTH = "accessTokenCookieAuth";

    @Bean
    public OpenAPI campusFlowOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("CampusFlow API")
                .version("v1"))
            .components(new Components()
                .addSecuritySchemes(
                    ACCESS_TOKEN_COOKIE_AUTH,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("ACCESS_TOKEN")
                ));
    }
}
