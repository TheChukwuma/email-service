package com.octopus.email_service.dto;

import com.octopus.email_service.entity.Email;
import com.octopus.email_service.enums.EmailStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResponse {
    
    private Long id;
    private UUID uuid;
    private String fromAddress;
    private List<String> toAddresses;
    private List<String> ccAddresses;
    private List<String> bccAddresses;
    private String subject;
    private String templateName;
    private String body;
    private EmailStatus status;
    private Integer attempts;
    private Integer maxAttempts;
    private String lastError;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<EmailEventResponse> events;
    
    public static EmailResponse fromEntity(Email email) {
        return EmailResponse.builder()
                .id(email.getId())
                .uuid(email.getUuid())
                .fromAddress(email.getFromAddress())
                .toAddresses(email.getToAddresses())
                .ccAddresses(email.getCcAddresses())
                .bccAddresses(email.getBccAddresses())
                .subject(email.getSubject())
//                .templateName(email.getTemplate() != null ? email.getTemplate().getName() : null)
                .body(email.getBody())
                .status(email.getStatus())
                .attempts(email.getAttempts())
                .maxAttempts(email.getMaxAttempts())
                .lastError(email.getLastError())
                .scheduledAt(email.getScheduledAt())
                .sentAt(email.getSentAt())
                .deliveredAt(email.getDeliveredAt())
                .createdAt(email.getCreatedAt())
                .updatedAt(email.getUpdatedAt())
                .build();
    }
}
