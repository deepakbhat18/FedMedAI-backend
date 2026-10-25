package com.fedlearn.medical.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final long ONE_MINUTE_MS = 60_000L;

    private static class RateTracker {
        final AtomicInteger requestCount = new AtomicInteger(0);
        final AtomicLong windowStartTime = new AtomicLong(System.currentTimeMillis());
    }

    private final Map<String, RateTracker> pathIpRateLimitMap = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        int maxAllowed = getMaxRequestsForPath(path);

        if (maxAllowed > 0) {
            String clientIp = getClientIP(request);
            String trackingKey = pathGroup(path) + ":" + clientIp;

            RateTracker tracker = pathIpRateLimitMap.computeIfAbsent(trackingKey, k -> new RateTracker());
            long currentTime = System.currentTimeMillis();

            if (currentTime - tracker.windowStartTime.get() > ONE_MINUTE_MS) {
                synchronized (tracker) {
                    if (currentTime - tracker.windowStartTime.get() > ONE_MINUTE_MS) {
                        tracker.requestCount.set(0);
                        tracker.windowStartTime.set(currentTime);
                    }
                }
            }

            if (tracker.requestCount.incrementAndGet() > maxAllowed) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(String.format("""
                        {
                            "success": false,
                            "message": "Too Many Requests - Rate limit exceeded. Maximum %d requests per minute allowed for this path.",
                            "status": 429
                        }
                        """, maxAllowed));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private int getMaxRequestsForPath(String path) {
        if (path.startsWith("/api/auth/")) {
            return 10;
        } else if (path.startsWith("/api/predictions")) {
            return 30;
        } else if (path.startsWith("/api/models/train") || path.startsWith("/api/models/trigger-training")) {
            return 5;
        } else if (path.startsWith("/api/")) {
            return 100;
        }
        return -1;
    }

    private String pathGroup(String path) {
        if (path.startsWith("/api/auth/")) return "AUTH";
        if (path.startsWith("/api/predictions")) return "PREDICTIONS";
        if (path.startsWith("/api/models/train") || path.startsWith("/api/models/trigger-training")) return "TRAIN";
        return "GENERAL";
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
