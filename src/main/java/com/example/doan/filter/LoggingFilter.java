package com.example.doan.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString() != null ? "?" + request.getQueryString() : "";
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }

        // Avoid logging noise for static assets or health checks if any, but log API requests
        boolean isApi = uri.startsWith("/api/");

        if (isApi) {
            log.debug(">>> [Bắt đầu Request API] {} {} - IP: {}", method, uri + queryString, clientIp);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (isApi) {
                long duration = System.currentTimeMillis() - startTime;
                int status = response.getStatus();
                
                String username = "anonymous";
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                    username = auth.getName();
                }

                log.info("<<< [Kết thúc Request API] {} {} - IP: {} - Trạng thái: {} - Người dùng: {} - Thời gian thực thi: {}ms",
                        method, uri + queryString, clientIp, status, username, duration);
            }
        }
    }
}
