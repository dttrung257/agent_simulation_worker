package com.uet.agent_simulation_worker.security;

import com.uet.agent_simulation_worker.security.filters.JwtAuthenticationFilter;
import com.uet.agent_simulation_worker.security.providers.AppAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * This class is used to configure security.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;
    private final AppAccessDeniedHandler appAccessDeniedHandler;
    private final AppAuthenticationProvider appAuthenticationProvider;
    private final AppAuthenticationEntrypoint appAuthenticationEntrypoint;

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(appAuthenticationProvider)
                .build();
    }

    /**
     * This bean is used to configure security.
     *
     * @param http - HttpSecurity
     * @return SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(
                        (exceptionHandling) -> exceptionHandling
                                .accessDeniedHandler(appAccessDeniedHandler)
                                .authenticationEntryPoint(appAuthenticationEntrypoint)
                )
                .authorizeHttpRequests((requests) -> requests
                        // Auth endpoints - public
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()

                        // Health check
                        .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/v1/experiment_result_images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/experiment_result_images/animation/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/experiment_results/**").permitAll()

                        .requestMatchers(HttpMethod.DELETE, "/api/v1/experiment_results/{id}/stop").permitAll()

                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
