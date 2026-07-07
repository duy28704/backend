package com.example.doan.config;

import com.example.doan.filter.JwtFilter;
import com.example.doan.filter.LoggingFilter;
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
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    private final RateLimitFilter rateLimitFilter;

    private final LoggingFilter loggingFilter;

     private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // disable csrf vì dùng JWT
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // stateless JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // public apis
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/error",
                                "/api/v1/orders/vnpay-callback"
                        ).permitAll()

                        // public OPTIONS requests for CORS preflight
                        .requestMatchers(
                                org.springframework.http.HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Users management APIs
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/v1/users"
                        ).hasAuthority("users.read")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/v1/users"
                        ).hasAuthority("users.create")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.PUT,
                                "/api/v1/users/**"
                        ).hasAuthority("users.update")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE,
                                "/api/v1/users/**"
                        ).hasAuthority("users.delete")

                        // Product endpoints (Trash & Restore)
                        .requestMatchers(
                                "/api/v1/products/deleted"
                        ).hasAuthority("product.trash")
                        .requestMatchers(
                                "/api/v1/products/restore",
                                "/api/v1/products/*/restore"
                        ).hasAuthority("product.restore")

                        // public products view APIs (GET only, excluding trash)
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/v1/products",
                                "/api/v1/products/**",
                                "/api/v1/search"
                        ).permitAll()

                        // Search and stats management APIs
                        .requestMatchers(
                                "/api/v1/search/stats"
                        ).hasAuthority("stats.view")
                        .requestMatchers(
                                "/api/v1/search/sync"
                        ).hasAuthority("product.update")
                        .requestMatchers(
                                "/api/v1/orders/dashboard-stats"
                        ).hasAuthority("order.manage")

                        // Inventory management APIs
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/v1/inventory/**"
                        ).hasAuthority("inventory.view")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/v1/inventory/**"
                        ).hasAuthority("inventory.manage")

                        // Product modifications
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/v1/products"
                        ).hasAuthority("product.create")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/v1/products/upload"
                        ).hasAnyAuthority("product.create", "product.update")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/v1/products/import"
                        ).hasAuthority("product.import")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.PUT,
                                "/api/v1/products/**"
                        ).hasAuthority("product.update")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE,
                                "/api/v1/products",
                                "/api/v1/products/**"
                        ).hasAuthority("product.delete")

                        // Generic ADMIN APIs
                        .requestMatchers(
                                "/api/v1/admin/**"
                        ).hasRole("ADMIN")

                        // Generic STAFF APIs
                        .requestMatchers(
                                "/api/v1/staff/**"
                        ).hasAnyRole("STAFF", "ADMIN")

                        // Generic CUSTOMER APIs
                        .requestMatchers(
                                "/api/v1/customers/**"
                        ).hasRole("CUSTOMER")

                        .anyRequest().authenticated()
                )

                // Logging Filter
                .addFilterBefore(
                        loggingFilter,
                        UsernamePasswordAuthenticationFilter.class
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