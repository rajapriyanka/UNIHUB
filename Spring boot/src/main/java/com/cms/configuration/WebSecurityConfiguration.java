package com.cms.configuration;

import com.cms.filter.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfiguration {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers("/api/student/login").permitAll()
                .requestMatchers("/api/email-actions/**").permitAll()
                .requestMatchers("/api/login", "/api/register", "/api/admin/default-exists", "/api/faculty/login", "/authenticate").permitAll()
                
                // Password reset endpoints - no authentication required
                .requestMatchers("/api/admin/password/reset-request").permitAll()
                .requestMatchers("/api/admin/password/reset-verify").permitAll()
                .requestMatchers("/api/admin/password/resend-otp").permitAll()
              .requestMatchers("/api/admin/password/change-email").permitAll()
                
                // Role-specific endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/student/profile/**").hasRole("STUDENT")
                .requestMatchers("/api/faculties/{facultyId}/courses/**").hasAnyRole("FACULTY","ADMIN")
                .requestMatchers("/api/faculty/**").hasAnyRole("ADMIN", "FACULTY","STUDENT")
                .requestMatchers("/api/timetable/**").hasAnyRole("ADMIN","FACULTY","STUDENT")
                
                // All other requests need authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Apply JWT filter but exclude password reset endpoints
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

