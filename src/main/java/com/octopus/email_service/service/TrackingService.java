package com.octopus.email_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.octopus.email_service.entity.Email;
import com.octopus.email_service.entity.EmailEvent;
import com.octopus.email_service.enums.EventType;
import com.octopus.email_service.repository.EmailEventRepository;
import com.octopus.email_service.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {
    
    private final EmailRepository emailRepository;
    private final EmailEventRepository emailEventRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public void trackEmailOpen(UUID emailUuid, String userAgent, InetAddress ipAddress) {
        Optional<Email> emailOpt = emailRepository.findByUuid(emailUuid);
        if (emailOpt.isEmpty()) {
            log.warn("Email not found for tracking open: {}", emailUuid);
            return;
        }
        
        Email email = emailOpt.get();
        
        // Check if this is the first open for this email
        boolean isFirstOpen = emailEventRepository.findByEmailIdAndEventType(email.getId(), EventType.OPEN).isEmpty();
        
        Map<String, Object> detail = new HashMap<>();
        detail.put("user_agent", userAgent);
        detail.put("ip_address", ipAddress != null ? ipAddress.getHostAddress() : null);
        detail.put("is_first_open", isFirstOpen);
        detail.put("timestamp", LocalDateTime.now());
        
        EmailEvent event = EmailEvent.builder()
                .email(email)
                .eventType(EventType.OPEN)
                .detail(serializeDetail(detail))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        
        emailEventRepository.save(event);
        log.info("Tracked email open for email ID: {} (first open: {})", email.getId(), isFirstOpen);
    }
    
    @Transactional
    public void trackEmailClick(UUID emailUuid, String targetUrl, String userAgent, InetAddress ipAddress) {
        Optional<Email> emailOpt = emailRepository.findByUuid(emailUuid);
        if (emailOpt.isEmpty()) {
            log.warn("Email not found for tracking click: {}", emailUuid);
            return;
        }
        
        Email email = emailOpt.get();
        
        Map<String, Object> detail = new HashMap<>();
        detail.put("target_url", targetUrl);
        detail.put("user_agent", userAgent);
        detail.put("ip_address", ipAddress != null ? ipAddress.getHostAddress() : null);
        detail.put("timestamp", LocalDateTime.now());
        
        EmailEvent event = EmailEvent.builder()
                .email(email)
                .eventType(EventType.CLICK)
                .detail(serializeDetail(detail))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        
        emailEventRepository.save(event);
        log.info("Tracked email click for email ID: {} to URL: {}", email.getId(), targetUrl);
    }
    
    public String generateTrackingPixelUrl(UUID emailUuid) {
        return "/track/open/" + emailUuid.toString();
    }
    
    public String generateClickTrackingUrl(UUID emailUuid, String targetUrl) {
        try {
            String encodedUrl = java.net.URLEncoder.encode(targetUrl, "UTF-8");
            return "/track/click/" + emailUuid.toString() + "?to=" + encodedUrl;
        } catch (Exception e) {
            log.error("Failed to encode target URL: {}", targetUrl, e);
            return targetUrl; // Return original URL if encoding fails
        }
    }
    
    public String injectTrackingIntoHtml(String htmlContent, UUID emailUuid) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return htmlContent;
        }
        
        // Add tracking pixel
        String trackingPixel = "<img src=\"" + generateTrackingPixelUrl(emailUuid) + 
                              "\" width=\"1\" height=\"1\" style=\"display:none;\" alt=\"\" />";
        
        // Inject before closing body tag
        if (htmlContent.contains("</body>")) {
            htmlContent = htmlContent.replace("</body>", trackingPixel + "</body>");
        } else {
            // If no body tag, append at the end
            htmlContent += trackingPixel;
        }
        
        return htmlContent;
    }
    
    public String injectClickTrackingIntoHtml(String htmlContent, UUID emailUuid) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return htmlContent;
        }
        
        // Find all href attributes and replace with tracking URLs
        String pattern = "href=\"([^\"]+)\"";
        java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = compiledPattern.matcher(htmlContent);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String originalUrl = matcher.group(1);
            // Skip if it's already a tracking URL or mailto link
            if (originalUrl.startsWith("/track/") || originalUrl.startsWith("mailto:")) {
                matcher.appendReplacement(result, matcher.group(0));
            } else {
                String trackingUrl = generateClickTrackingUrl(emailUuid, originalUrl);
                matcher.appendReplacement(result, "href=\"" + trackingUrl + "\"");
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String serializeDetail(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            log.error("Failed to serialize tracking detail", e);
            return "{}";
        }
    }
}
