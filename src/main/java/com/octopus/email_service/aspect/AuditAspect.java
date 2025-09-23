package com.octopus.email_service.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.octopus.email_service.annotation.Auditable;
import com.octopus.email_service.entity.AuditLog;
import com.octopus.email_service.entity.User;
import com.octopus.email_service.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    
    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // Get current user info
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = null;
        String username = "anonymous";
        String userRole = "UNKNOWN";
        
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            currentUser = (User) authentication.getPrincipal();
            username = currentUser.getUsername();
            userRole = currentUser.getRole().name();
        }
        
        // Get request info
        String ipAddress = null;
        String userAgent = null;
        
        try {
            ServletRequestAttributes requestAttributes = 
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = requestAttributes.getRequest();
            
            ipAddress = getClientIpAddress(request);
            userAgent = request.getHeader("User-Agent");
        } catch (Exception e) {
            log.debug("Could not get request attributes for audit log", e);
        }
        
        // Extract resource ID from method arguments if possible
        String resourceId = extractResourceId(joinPoint.getArgs());
        
        // Prepare audit details
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("method", joinPoint.getSignature().getName());
        auditDetails.put("class", joinPoint.getTarget().getClass().getSimpleName());
        auditDetails.put("arguments", sanitizeArguments(joinPoint.getArgs()));
        auditDetails.put("executionTime", 0L); // Will be updated after execution
        
        AuditLog auditLog = AuditLog.builder()
                .user(currentUser)
                .username(username)
                .userRole(userRole)
                .action(auditable.action())
                .resourceType(auditable.resourceType().isEmpty() ? 
                             extractResourceTypeFromMethod(joinPoint) : auditable.resourceType())
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(true)
                .build();
        
        Object result = null;
        try {
            // Execute the method
            result = joinPoint.proceed();
            
            // Update execution time
            long executionTime = System.currentTimeMillis() - startTime;
            auditDetails.put("executionTime", executionTime);
            
            // Add result info if it's not sensitive
            if (result != null && !isSensitiveResult(result)) {
                auditDetails.put("resultType", result.getClass().getSimpleName());
                if (result instanceof Number) {
                    auditDetails.put("resultValue", result);
                }
            }
            
            auditLog.setSuccess(true);
            
        } catch (Exception e) {
            // Log the error
            auditLog.setSuccess(false);
            auditLog.setErrorMessage(e.getMessage());
            auditDetails.put("error", e.getClass().getSimpleName());
            auditDetails.put("errorMessage", e.getMessage());
            
            throw e; // Re-throw the exception
        } finally {
            try {
                // Set audit details as JSON
                auditLog.setDetails(objectMapper.writeValueAsString(auditDetails));
                
                // Save audit log asynchronously to avoid performance impact
                saveAuditLogAsync(auditLog);
                
            } catch (Exception e) {
                log.error("Failed to save audit log", e);
            }
        }
        
        return result;
    }
    
    private void saveAuditLogAsync(AuditLog auditLog) {
        // In a real application, you might want to use @Async here
        // For now, we'll save synchronously but catch any exceptions
        try {
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to persist audit log", e);
        }
    }
    
    private String extractResourceId(Object[] args) {
        // Try to extract ID from common parameter patterns
        for (Object arg : args) {
            if (arg instanceof Long || arg instanceof String) {
                return arg.toString();
            }
            // Check if it's an object with an ID field
            if (arg != null) {
                try {
                    var idField = arg.getClass().getDeclaredField("id");
                    idField.setAccessible(true);
                    Object id = idField.get(arg);
                    if (id != null) {
                        return id.toString();
                    }
                } catch (Exception e) {
                    // Ignore reflection errors
                }
            }
        }
        return null;
    }
    
    private String extractResourceTypeFromMethod(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // Extract resource type from class name (e.g., TemplateService -> TEMPLATE)
        if (className.endsWith("Service")) {
            return className.replace("Service", "").toUpperCase();
        } else if (className.endsWith("Controller")) {
            return className.replace("Controller", "").toUpperCase();
        }
        
        return "UNKNOWN";
    }
    
    private Object[] sanitizeArguments(Object[] args) {
        // Remove sensitive data from arguments before logging
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) return null;
                    
                    String argStr = arg.toString();
                    // Don't log password or sensitive fields
                    if (argStr.toLowerCase().contains("password") || 
                        argStr.toLowerCase().contains("secret") ||
                        argStr.toLowerCase().contains("token")) {
                        return "[REDACTED]";
                    }
                    
                    // Limit argument length in logs
                    if (argStr.length() > 200) {
                        return argStr.substring(0, 200) + "...";
                    }
                    
                    return arg;
                })
                .toArray();
    }
    
    private boolean isSensitiveResult(Object result) {
        // Check if result contains sensitive data that shouldn't be logged
        String resultStr = result.toString().toLowerCase();
        return resultStr.contains("password") || 
               resultStr.contains("secret") || 
               resultStr.contains("token") ||
               resultStr.contains("key");
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
