package com.example.CampusFlowServer.domain.auth.config;

import com.example.CampusFlowServer.domain.auth.cookie.AuthCookieManager;
import com.example.CampusFlowServer.domain.auth.filter.CsrfCookieFilter;
import com.example.CampusFlowServer.domain.auth.filter.JwtAuthenticationFilter;
import com.example.CampusFlowServer.domain.auth.filter.LoginFilter;
import com.example.CampusFlowServer.domain.auth.handler.AuthAccessDeniedHandler;
import com.example.CampusFlowServer.domain.auth.handler.AuthAuthenticationEntryPoint;
import com.example.CampusFlowServer.domain.auth.handler.AuthSecurityResponseWriter;
import com.example.CampusFlowServer.domain.auth.properties.CookieProperties;
import com.example.CampusFlowServer.domain.auth.service.TokenService;
import com.example.CampusFlowServer.global.config.properties.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CookieProperties cookieProperties;
    private final CorsProperties corsProperties;
    private final AuthAuthenticationEntryPoint authenticationEntryPoint;
    private final AuthAccessDeniedHandler accessDeniedHandler;
    private final ObjectMapper objectMapper;
    private final TokenService tokenService;
    private final AuthCookieManager authCookieManager;
    private final AuthSecurityResponseWriter responseWriter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        AuthenticationManager authenticationManager
    ) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieName(cookieProperties.getCsrfTokenName());
        csrfTokenRepository.setHeaderName(cookieProperties.getCsrfHeaderName());
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie
            .secure(cookieProperties.isSecure())
            .sameSite(cookieProperties.getSameSite())
            .path("/")
        );
        CsrfTokenRequestAttributeHandler csrfTokenRequestHandler =
            new CsrfTokenRequestAttributeHandler();

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(csrfTokenRequestHandler)
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .formLogin(formLogin -> formLogin.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            .logout(logout -> logout.disable())
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/auth/reissue",
                    "/api/v1/auth/logout",
                    "/api/v1/csrf"
                ).permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAt(
                new LoginFilter(
                    authenticationManager,
                    objectMapper,
                    tokenService,
                    authCookieManager,
                    responseWriter
                ),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
