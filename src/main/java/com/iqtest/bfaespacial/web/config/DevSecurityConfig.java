package com.iqtest.bfaespacial.web.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * DEV & DEMO ONLY security configuration.
 * Enables full end-to-end testing of authentication, custom login UI, BCrypt, and CSRF token flow.
 */
@Configuration
@Profile({"dev", "demo"})
public class DevSecurityConfig {

    @Bean
    SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/react/**", "/img/**", "/error").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/integracion/**").permitAll()
                        .requestMatchers("/evaluacion/**", "/api/subtest/**", "/api/respuesta/**").hasRole("ESTUDIANTE")
                        .requestMatchers("/resultados/**").hasRole("EVALUADOR")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/evaluacion/inicio", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                // Enable CSRF and use a cookie repository accessible by Javascript (React)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                // Add filter to resolve deferred CSRF token so the cookie gets generated on GET requests
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder devPasswordEncoder() {
        return new BCryptPasswordEncoder(12); // Strength 12 as per OWASP checklist
    }

    @Bean
    UserDetailsService devUsers(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("estudiante")
                        .password(encoder.encode("estudiante123"))
                        .roles("ESTUDIANTE")
                        .build(),
                User.withUsername("evaluador")
                        .password(encoder.encode("evaluador123"))
                        .roles("EVALUADOR")
                        .build(),
                User.withUsername("admin")
                        .password(encoder.encode("admin123"))
                        .roles("ADMIN")
                        .build()
        );
    }

    /**
     * Filter to force generation of the deferred CSRF token on initial page loads.
     */
    private static class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                // Fetch token to trigger serialization into the response cookie
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
