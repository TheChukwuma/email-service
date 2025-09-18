package com.octopus.email_service.dto;

import com.octopus.email_service.entity.EmailEvent;
import com.octopus.email_service.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEventResponse {
    
    private Long id;
    private EventType eventType;
    private String detail;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
    
    public static EmailEventResponse fromEntity(EmailEvent event) {
        return EmailEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .detail(event.getDetail())
                .ipAddress(event.getIpAddress() != null ? event.getIpAddress().getHostAddress() : null)
                .userAgent(event.getUserAgent())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
