package com.ilog.global.config;

import com.ilog.global.security.JwtAuthenticationFilter;
import com.ilog.global.security.JwtProvider;
import com.ilog.global.security.LoginMemberLoader;
import com.ilog.global.security.PasswordResetRequiredFilter;
import com.ilog.global.security.RestAccessDeniedHandler;
import com.ilog.global.security.RestAuthenticationEntryPoint;
import com.ilog.global.security.SecurityErrorResponseWriter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final LoginMemberLoader loginMemberLoader;
    private final JsonMapper jsonMapper;
    private final CorsProperties corsProperties;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        SecurityErrorResponseWriter errorResponseWriter =
                new SecurityErrorResponseWriter(jsonMapper);
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                                new RestAuthenticationEntryPoint(
                                                        errorResponseWriter))
                                        .accessDeniedHandler(
                                                new RestAccessDeniedHandler(errorResponseWriter)))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(HttpMethod.POST, "/api/v1/users")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/v1/users/email-availability",
                                                "/api/v1/users/nickname-availability")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/v1/auth/tokens",
                                                "/api/v1/auth/temporary-passwords")
                                        .permitAll()
                                        .requestMatchers(
                                                "/actuator/health",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/v3/api-docs/**",
                                                "/error")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, loginMemberLoader),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(
                        new PasswordResetRequiredFilter(errorResponseWriter),
                        AuthorizationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of(HttpHeaders.LOCATION));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
