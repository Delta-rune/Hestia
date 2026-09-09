package com.hestia.vault.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for API testing
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/auth/**", "/static/**", "/*.html", "/*.css", "/*.js", "/favicon.ico").permitAll() // Allow public access to index.html and auth APIs
                        .anyRequest().permitAll() // Allow all endpoints for demo
                );
        return http.build();
    }
}