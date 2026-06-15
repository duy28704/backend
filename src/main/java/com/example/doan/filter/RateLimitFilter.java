package com.example.doan.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter
        extends OncePerRequestFilter {

    // lưu bucket theo IP
    private final Map<String, Bucket>
            cache = new ConcurrentHashMap<>();

    // tạo bucket
    private Bucket createBucket() {

        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillGreedy(10, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // lấy IP client
            String ip = request.getRemoteAddr();
            if (ip == null || ip.isEmpty()) {
                ip = "unknown";
            }

            // lấy bucket theo IP
            Bucket bucket = cache.computeIfAbsent(
                    ip,
                    k -> createBucket()
            );

            // còn token?
            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                // quá giới hạn request
                response.setStatus(429);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"status\": 429, \"message\": \"Too Many Requests / Yêu cầu quá thường xuyên\", \"data\": null}");
            }
        } catch (Exception e) {
            System.err.println("[RateLimitFilter] Error checking rate limit: " + e.getMessage());
            filterChain.doFilter(request, response);
        }
    }
}
