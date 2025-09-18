package com.octopus.email_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.octopus.email_service.dto.EmailRequest;
import com.octopus.email_service.dto.EmailResponse;
import com.octopus.email_service.entity.Email;
import com.octopus.email_service.entity.EmailEvent;
import com.octopus.email_service.entity.Template;
import com.octopus.email_service.enums.EmailStatus;
import com.octopus.email_service.enums.EventType;
import com.octopus.email_service.repository.EmailEventRepository;
import com.octopus.email_service.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final EmailRepository emailRepository;
    private final EmailEventRepository emailEventRepository;
    private final TemplateService templateService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${app.email.queue.name:email.queue}")
    private String emailQueueName;
    
    @Value("${app.email.exchange.name:email.exchange}")
    private String emailExchangeName;
    
    @Value("${app.email.routing.key:email.send}")
    private String emailRoutingKey;
    
    @Transactional
    public EmailResponse sendEmail(EmailRequest request) {
        // Validate template if provided
        Template template = null;
        if (request.getTemplateName() != null) {
            template = templateService.getTemplateEntityByName(request.getTemplateName())
                    .orElseThrow(() -> new IllegalArgumentException("Template not found: " + request.getTemplateName()));
        }
        
        // Create email entity
        Email email = Email.builder()
                .fromAddress(request.getFrom())
                .toAddress(request.getTo())
                .ccAddresses(request.getCc())
                .bccAddresses(request.getBcc())
                .subject(request.getSubject())
                .template(template)
                .templateVars(serializeTemplateVars(request.getTemplateVars()))
                .body(request.getBody())
                .attachments(serializeAttachments(request.getAttachments()))
                .status(EmailStatus.ENQUEUED)
                .build();
        
        Email savedEmail = emailRepository.save(email);
        
        // Create initial event
        createEmailEvent(savedEmail, EventType.ENQUEUED, null);
        
        // Send to queue
        sendToQueue(savedEmail.getId());
        
        log.info("Email enqueued with ID: {} for recipient: {}", savedEmail.getId(), request.getTo());
        
        return EmailResponse.fromEntity(savedEmail);
    }
    
    public Optional<EmailResponse> getEmailById(Long id) {
        return emailRepository.findById(id)
                .map(EmailResponse::fromEntity);
    }
    
    public Optional<EmailResponse> getEmailByUuid(UUID uuid) {
        return emailRepository.findByUuid(uuid)
                .map(EmailResponse::fromEntity);
    }
    
    public Page<EmailResponse> getEmailsByStatus(EmailStatus status, Pageable pageable) {
        return emailRepository.findByStatus(status, pageable)
                .map(EmailResponse::fromEntity);
    }
    
    public Page<EmailResponse> getEmailsByRecipient(String toAddress, Pageable pageable) {
        return emailRepository.findByToAddress(toAddress, pageable)
                .map(EmailResponse::fromEntity);
    }
    
    public Page<EmailResponse> getEmailsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return emailRepository.findByCreatedAtBetween(startDate, endDate, pageable)
                .map(EmailResponse::fromEntity);
    }
    
    public List<Email> getEmailsForProcessing() {
        return emailRepository.findEmailsForProcessing(EmailStatus.ENQUEUED, LocalDateTime.now());
    }
    
    @Transactional
    public void updateEmailStatus(Long emailId, EmailStatus status, String error) {
        emailRepository.updateEmailStatusAndAttempts(emailId, status, error);
        
        Email email = emailRepository.findById(emailId).orElse(null);
        if (email != null) {
            createEmailEvent(email, mapStatusToEventType(status), error);
        }
    }
    
    @Transactional
    public void markAsSent(Long emailId) {
        emailRepository.markAsSent(emailId, EmailStatus.SENT, LocalDateTime.now());
        
        Email email = emailRepository.findById(emailId).orElse(null);
        if (email != null) {
            createEmailEvent(email, EventType.SENT, null);
        }
    }
    
    @Transactional
    public void markAsDelivered(Long emailId) {
        emailRepository.markAsDelivered(emailId, EmailStatus.DELIVERED, LocalDateTime.now());
        
        Email email = emailRepository.findById(emailId).orElse(null);
        if (email != null) {
            createEmailEvent(email, EventType.DELIVERED, null);
        }
    }
    
    public long getEmailCountByStatus(EmailStatus status) {
        return emailRepository.countByStatus(status);
    }
    
    public long getEmailCountSince(LocalDateTime since) {
        return emailRepository.countCreatedSince(since);
    }
    
    private void sendToQueue(Long emailId) {
        try {
            rabbitTemplate.convertAndSend(emailExchangeName, emailRoutingKey, emailId);
            log.debug("Email ID {} sent to queue", emailId);
        } catch (Exception e) {
            log.error("Failed to send email ID {} to queue", emailId, e);
            updateEmailStatus(emailId, EmailStatus.FAILED, "Failed to enqueue: " + e.getMessage());
        }
    }
    
    private void createEmailEvent(Email email, EventType eventType, String detail) {
        try {
            EmailEvent event = EmailEvent.builder()
                    .email(email)
                    .eventType(eventType)
                    .detail(detail)
                    .build();
            
            emailEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to create email event for email ID: {}", email.getId(), e);
        }
    }
    
    private String serializeTemplateVars(Object templateVars) {
        if (templateVars == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(templateVars);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize template variables", e);
            return null;
        }
    }
    
    private String serializeAttachments(Object attachments) {
        if (attachments == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attachments);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize attachments", e);
            return null;
        }
    }
    
    private EventType mapStatusToEventType(EmailStatus status) {
        return switch (status) {
            case ENQUEUED -> EventType.ENQUEUED;
            case SENDING -> EventType.SENDING;
            case SENT -> EventType.SENT;
            case DELIVERED -> EventType.DELIVERED;
            case FAILED -> EventType.FAILED;
            case BOUNCED -> EventType.BOUNCED;
        };
    }
}
