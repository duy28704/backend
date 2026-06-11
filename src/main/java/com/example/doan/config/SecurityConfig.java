package com.example.doan.config;

import com.example.doan.filter.JwtFilter;
import com.example.doan.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    private final RateLimitFilter rateLimitFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // disable csrf vì dùng JWT
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // stateless JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // public apis
                        .requestMatchers(
                                "/api/v1/auth/**"
                        ).permitAll()

                        // public products view APIs
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/v1/products",
                                "/api/v1/products/**"
                        ).permitAll()

                        // ADMIN product modifications (CRUD/Import)
                        .requestMatchers(
                                "/api/v1/products/**"
                        ).hasAuthority("ADMIN")

                        // ADMIN APIs
                        .requestMatchers(
                                "/api/v1/admin/**"
                        ).hasAuthority("ADMIN")

                        // STAFF APIs
                        .requestMatchers(
                                "/api/v1/staff/**"
                        ).hasAnyAuthority(
                                "STAFF",
                                "ADMIN"
                        )

                        // CUSTOMER APIs
                        .requestMatchers(
                                "/api/v1/customers/**"
                        ).hasAuthority("CUSTOMER")

                        .anyRequest().authenticated()
                )

                // Rate Limit Filter
                .addFilterBefore(
                        rateLimitFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // JWT Filter
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }
}