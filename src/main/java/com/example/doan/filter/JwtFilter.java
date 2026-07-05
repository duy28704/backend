package com.example.doan.filter;

import com.example.doan.service.CustomUserDetailsService;
import com.example.doan.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private final CustomUserDetailsService
            userDetailsService;

    @Override
    protected void doFilterInternal(

            HttpServletRequest request,

            HttpServletResponse response,

            FilterChain filterChain

    ) throws ServletException, IOException {

        String authHeader =
                request.getHeader(
                        "Authorization"
                );

        String token = null;

        String username = null;

        // kiểm tra Bearer token
        if (authHeader != null
                && authHeader.startsWith(
                "Bearer "
        )) {
            try {
                token = authHeader.substring(7);
                username = jwtUtil.extractUsername(token);
            } catch (Exception e) {
                log.error("Lỗi phân tích mã token JWT: {}", e.getMessage());
            }
        }

        // nếu chưa authenticate
        if (username != null
                && SecurityContextHolder
                .getContext()
                .getAuthentication() == null) {

            UserDetails userDetails =
                    userDetailsService
                            .loadUserByUsername(
                                    username
                            );

            // validate JWT
            if (jwtUtil.validateToken(token)) {

                UsernamePasswordAuthenticationToken
                        authentication =
                        new UsernamePasswordAuthenticationToken(

                                userDetails,

                                null,

                                userDetails
                                        .getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                // set security context
                SecurityContextHolder
                        .getContext()
                        .setAuthentication(
                                authentication
                        );
            }
        }

        // cho request đi tiếp
        filterChain.doFilter(
                request,
                response
        );
    }
}