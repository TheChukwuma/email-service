package com.octopus.email_service.controller;

import com.octopus.email_service.service.TrackingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

@RestController
@RequestMapping("/track")
@RequiredArgsConstructor
@Slf4j
public class TrackingController {
    
    private final TrackingService trackingService;
    
    @GetMapping("/open/{emailUuid}")
    public ResponseEntity<byte[]> trackEmailOpen(
            @PathVariable UUID emailUuid,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        try {
            String userAgent = request.getHeader("User-Agent");
            InetAddress ipAddress = getClientIpAddress(request);
            
            trackingService.trackEmailOpen(emailUuid, userAgent, ipAddress);
            
            // Return a 1x1 transparent pixel
            byte[] pixel = createTransparentPixel();
            
            response.setContentType(MediaType.IMAGE_PNG_VALUE);
            response.setContentLength(pixel.length);
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            
            return ResponseEntity.ok(pixel);
            
        } catch (Exception e) {
            log.error("Failed to track email open for UUID: {}", emailUuid, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/click/{emailUuid}")
    public void trackEmailClick(
            @PathVariable UUID emailUuid,
            @RequestParam String to,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        try {
            String userAgent = request.getHeader("User-Agent");
            InetAddress ipAddress = getClientIpAddress(request);
            
            trackingService.trackEmailClick(emailUuid, to, userAgent, ipAddress);
            
            // Redirect to the target URL
            response.sendRedirect(to);
            
        } catch (Exception e) {
            log.error("Failed to track email click for UUID: {} to URL: {}", emailUuid, to, e);
            response.sendRedirect(to); // Still redirect even if tracking fails
        }
    }
    
    private InetAddress getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String xRealIp = request.getHeader("X-Real-IP");
        String remoteAddr = request.getRemoteAddr();
        
        String clientIp = null;
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            clientIp = xForwardedFor.split(",")[0].trim();
        } else if (xRealIp != null && !xRealIp.isEmpty()) {
            clientIp = xRealIp;
        } else {
            clientIp = remoteAddr;
        }
        
        try {
            return InetAddress.getByName(clientIp);
        } catch (UnknownHostException e) {
            log.warn("Failed to resolve IP address: {}", clientIp);
            return null;
        }
    }
    
    private byte[] createTransparentPixel() {
        // Simple 1x1 transparent GIF pixel
        return new byte[]{
            (byte) 0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00, 
            (byte) 0x80, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00, 
            (byte) 0x21, (byte) 0xF9, 0x04, 0x01, 0x00, 0x00, 0x00, 0x00, 0x2C, 0x00, 
            0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x02, 0x01, 0x44, 0x00, 0x3B
        };
    }
}
