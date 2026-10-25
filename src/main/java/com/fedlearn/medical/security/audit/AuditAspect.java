package com.fedlearn.medical.security.audit;

import com.fedlearn.medical.entity.AuditLog;
import com.fedlearn.medical.entity.User;
import com.fedlearn.medical.repository.AuditLogRepository;
import com.fedlearn.medical.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditAspect.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    @Async("taskExecutor")
    public void logAuditAction(JoinPoint joinPoint, Audited audited, Object result) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            Long userId = null;
            String username = "ANONYMOUS";
            Long hospitalId = null;

            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                username = auth.getName();
                User user = userRepository.findByUsername(username).orElse(null);
                if (user != null) {
                    userId = user.getId();
                }
            }

            String ipAddress = "0.0.0.0";
            String userAgent = "UNKNOWN";

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = getClientIP(request);
                userAgent = request.getHeader("User-Agent");
                if (userAgent != null && userAgent.length() > 255) {
                    userAgent = userAgent.substring(0, 255);
                }
            }

            String resourceId = extractResourceId(joinPoint);

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .hospitalId(hospitalId)
                    .action(audited.action())
                    .resourceType(audited.resourceType())
                    .resourceId(resourceId)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(auditLog);

            logger.info("[AUDIT LOG] User: {} | Action: {} | Resource: {}/{} | IP: {}",
                    username, audited.action(), audited.resourceType(), resourceId, ipAddress);

        } catch (Exception e) {
            logger.error("Non-fatal exception during asynchronous audit logging: {}", e.getMessage(), e);
        }
    }

    private String extractResourceId(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg instanceof Long || arg instanceof String || arg instanceof Integer) {
                    return String.valueOf(arg);
                }
            }
        }
        return "N/A";
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
