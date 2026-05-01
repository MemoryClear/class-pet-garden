package com.classpet.config;

import com.classpet.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Exact same paths as SpaController @GetMapping to ensure they all hit permitAll
    private static final String[] SPA_ROUTES = {
        "/", "/home", "/activate", "/history",
        "/exchange-history", "/leaderboard", "/shop", "/settings"
    };

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // SPA frontend routes - must match SpaController exactly
                .requestMatchers(SPA_ROUTES).permitAll()
                // Static resources from Vite build
                .requestMatchers("/assets/**", "/static/**",
                    "/*.html", "/*.js", "/*.css", "/*.ico",
                    "/*.png", "/*.jpg", "/*.svg").permitAll()
                // Public APIs (no auth needed)
                .requestMatchers("/api/settings").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/pets", "/api/pets/**").permitAll()
                .requestMatchers("/error").permitAll()
                // All other API endpoints require authentication
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authEx) -> {
                    System.out.println("[403 DEBUG] URI: " + req.getRequestURI() + " AuthEx: " + authEx.getClass().getName() + " - " + authEx.getMessage());
                    res.sendError(403, authEx.getMessage());
                })
                .accessDeniedHandler((req, res, accessEx) -> {
                    System.out.println("[403 DEBUG] URI: " + req.getRequestURI() + " AccessEx: " + accessEx.getClass().getName() + " - " + accessEx.getMessage());
                    res.sendError(403, accessEx.getMessage());
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}