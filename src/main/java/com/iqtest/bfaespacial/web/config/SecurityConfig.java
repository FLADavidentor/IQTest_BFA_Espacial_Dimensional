package com.iqtest.bfaespacial.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Session-based security (§8). Roles: ESTUDIANTE, EVALUADOR, ADMIN.
 * Auth is owned by IQTest (§17); these in-memory users are a DEV placeholder
 * until real session integration in Phase 6. // STUB — Phase 6
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/react/**", "/img/**", "/error")
                            .permitAll()
                        // Actuator is NOT public (7-B) — ADMIN only, in every profile.
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // service-to-service: token validated in the controller (§9, §19 Q4)
                        .requestMatchers("/api/integracion/**").permitAll()
                        .requestMatchers("/evaluacion/**", "/api/subtest/**", "/api/respuesta/**")
                            .hasRole("ESTUDIANTE")
                        .requestMatchers("/resultados/**").hasRole("EVALUADOR")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
                // SPA endpoints use session auth; CSRF token flow is out of scope for the embedded React.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
        return http.build();
    }

    /** DEV ONLY — replaced by IQTest session integration in Phase 6. */
    @Bean
    UserDetailsService devUsers() {
        return new InMemoryUserDetailsManager(
                User.withUsername("estudiante").password("{noop}x").roles("ESTUDIANTE").build(),
                User.withUsername("evaluador").password("{noop}x").roles("EVALUADOR").build(),
                User.withUsername("admin").password("{noop}x").roles("ADMIN").build());
    }
}
