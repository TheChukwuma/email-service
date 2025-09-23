package com.octopus.email_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.octopus.email_service.dto.AttachmentResponse;
import com.octopus.email_service.dto.AttachmentUploadRequest;
import com.octopus.email_service.dto.EmailAttachmentDto;
import com.octopus.email_service.dto.EmailRequest;
import com.octopus.email_service.dto.EmailResponse;
import com.octopus.email_service.entity.Attachment;
import com.octopus.email_service.entity.Email;
import com.octopus.email_service.entity.EmailEvent;
import com.octopus.email_service.entity.EmailTenant;
import com.octopus.email_service.entity.Template;
import com.octopus.email_service.enums.EmailStatus;
import com.octopus.email_service.enums.EventType;
import com.octopus.email_service.repository.AttachmentRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final EmailRepository emailRepository;
    private final EmailEventRepository emailEventRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentService attachmentService;
    private final TemplateService templateService;
    private final EmailTenantService emailTenantService;
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
        
        // Process attachments if provided
        String attachmentsJson = null;
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            List<Attachment> processedAttachments = processEmailAttachments(request.getAttachments(), request.getFrom());
            attachmentsJson = serializeProcessedAttachments(processedAttachments);
        }
        
        // Determine if we need to use a fallback template for plain text bodies
        String processedBody = request.getBody();
        boolean isHtmlBody = isHtmlContent(request.getBody());
        boolean needsFallbackTemplate = template == null && !isHtmlBody && request.getBody() != null;
        
        // Create email entity
        Email email = Email.builder()
                .fromAddress(request.getFrom())
                .toAddresses(request.getTo())
                .ccAddresses(request.getCc())
                .bccAddresses(request.getBcc())
                .subject(request.getSubject())
                .template(template)
                .templateVars(serializeTemplateVars(request.getTemplateVars()))
                .body(processedBody)
                .attachments(attachmentsJson)
                .status(EmailStatus.ENQUEUED)
                .isHtmlBody(isHtmlBody)
                .needsFallbackTemplate(needsFallbackTemplate)
                .build();
        
        Email savedEmail = emailRepository.save(email);
        
        // Create initial event
        createEmailEvent(savedEmail, EventType.ENQUEUED, null);
        
        // Send to queue after transaction commits to avoid race condition
        // We'll use the email ID that's already been saved
        final Long emailIdToQueue = savedEmail.getId();
        
        // Use TransactionSynchronization to send to queue after commit
        org.springframework.transaction.support.TransactionSynchronizationManager
            .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendToQueue(emailIdToQueue);
                }
            });
        
        log.info("Email enqueued with ID: {} for recipients: {} with {} attachments", 
                savedEmail.getId(), String.join(", ", request.getTo()), 
                request.getAttachments() != null ? request.getAttachments().size() : 0);
        
        return EmailResponse.fromEntity(savedEmail);
    }
    
    /**
     * Send email with tenant-based sender configuration
     */
    @Transactional
    public EmailResponse sendEmail(EmailRequest request, EmailTenant tenant) {
        // Validate template if provided
        Template template = null;
        if (request.getTemplateName() != null) {
            template = templateService.getTemplateEntityByName(request.getTemplateName())
                    .orElseThrow(() -> new IllegalArgumentException("Template not found: " + request.getTemplateName()));
        }
        
        // Determine the effective sender address based on tenant configuration
        String effectiveSenderAddress = determineSenderAddress(request.getFrom(), tenant);
        
        // Determine the effective reply-to address based on tenant configuration
        String effectiveReplyToAddress = determineReplyToAddress(request.getFrom(), tenant);
        
        // Process attachments if provided
        String attachmentsJson = null;
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            List<Attachment> processedAttachments = processEmailAttachments(request.getAttachments(), effectiveSenderAddress);
            attachmentsJson = serializeProcessedAttachments(processedAttachments);
        }
        
        // Determine if we need to use a fallback template for plain text bodies
        String processedBody = request.getBody();
        boolean isHtmlBody = isHtmlContent(request.getBody());
        boolean needsFallbackTemplate = template == null && !isHtmlBody && request.getBody() != null;
        
        // Create email entity
        Email email = Email.builder()
                .fromAddress(effectiveSenderAddress)
                .replyToAddress(effectiveReplyToAddress)
                .toAddresses(request.getTo())
                .ccAddresses(request.getCc())
                .bccAddresses(request.getBcc())
                .subject(request.getSubject())
                .template(template)
                .templateVars(serializeTemplateVars(request.getTemplateVars()))
                .body(processedBody)
                .attachments(attachmentsJson)
                .status(EmailStatus.ENQUEUED)
                .isHtmlBody(isHtmlBody)
                .needsFallbackTemplate(needsFallbackTemplate)
                .build();
        
        Email savedEmail = emailRepository.save(email);
        
        // Create initial event
        createEmailEvent(savedEmail, EventType.ENQUEUED, null);
        
        // Send to queue after transaction commits to avoid race condition
        final Long emailIdToQueue = savedEmail.getId();
        
        // Use TransactionSynchronization to send to queue after commit
        org.springframework.transaction.support.TransactionSynchronizationManager
            .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendToQueue(emailIdToQueue);
                }
            });
        
        log.info("Email enqueued with ID: {} for recipients: {} from tenant: {} with {} attachments", 
                savedEmail.getId(), String.join(", ", request.getTo()), 
                tenant != null ? tenant.getTenantCode() : "none",
                request.getAttachments() != null ? request.getAttachments().size() : 0);
        
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
    
    
    private List<Attachment> processEmailAttachments(List<EmailAttachmentDto> emailAttachments, String createdBy) {
        List<Attachment> processedAttachments = new ArrayList<>();
        
        for (EmailAttachmentDto emailAttachment : emailAttachments) {
            try {
                // Validate base64 content
                validateBase64Attachment(emailAttachment);
                
                // Create attachment upload request
                AttachmentUploadRequest uploadRequest = AttachmentUploadRequest.builder()
                        .storageType(emailAttachment.getStorageType() != null ? 
                            emailAttachment.getStorageType() : 
                            AttachmentUploadRequest.StorageType.AUTO)
                        .optimizeImage(emailAttachment.getOptimizeImage())
                        .generateThumbnail(emailAttachment.getGenerateThumbnail())
                        .expiresAt(LocalDateTime.now().plusHours(24)) // Default 24 hours
                        .build();
                
                // Upload attachment
                AttachmentResponse attachmentResponse = attachmentService.uploadBase64Attachment(
                        emailAttachment.getBase64Content(),
                        emailAttachment.getFilename() != null ? emailAttachment.getFilename() : "attachment",
                        emailAttachment.getMimeType(),
                        uploadRequest,
                        createdBy
                );
                
                // Get the full attachment entity and update with email-specific fields
                Attachment attachment = attachmentRepository.findById(attachmentResponse.getId())
                        .orElseThrow(() -> new RuntimeException("Failed to retrieve uploaded attachment"));
                
                // Update with email-specific metadata
                attachment.setIsInline(emailAttachment.getInline() != null ? emailAttachment.getInline() : false);
                attachment.setContentId(emailAttachment.getCid());
                attachment.setDescription(emailAttachment.getDescription());
                
                // Save updated attachment
                attachment = attachmentRepository.save(attachment);
                processedAttachments.add(attachment);
                
                log.debug("Processed email attachment: {} (inline: {}, cid: {})", 
                        attachment.getOriginalFilename(), 
                        attachment.getIsInline(), 
                        attachment.getContentId());
                
            } catch (Exception e) {
                log.error("Failed to process email attachment: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to process attachment: " + e.getMessage(), e);
            }
        }
        
        return processedAttachments;
    }
    
    private String serializeProcessedAttachments(List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attachments);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize processed attachments", e);
            return null;
        }
    }
    
    private void validateBase64Attachment(EmailAttachmentDto emailAttachment) {
        // Validate base64 content
        if (emailAttachment.getBase64Content() == null || emailAttachment.getBase64Content().trim().isEmpty()) {
            throw new IllegalArgumentException("Base64 content cannot be empty");
        }
        
        // Validate base64 format
        try {
            Base64.getDecoder().decode(emailAttachment.getBase64Content());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid base64 content format", e);
        }
        
        // Validate file size
        byte[] content = Base64.getDecoder().decode(emailAttachment.getBase64Content());
        if (content.length > 10 * 1024 * 1024) { // 10MB limit
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }
        
        // Validate MIME type if provided
        if (emailAttachment.getMimeType() != null) {
            String[] allowedTypes = {
                "image/jpeg", "image/png", "image/gif", "image/webp",
                "application/pdf", "text/plain", "text/csv",
                "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            };
            
            boolean isValidType = Arrays.stream(allowedTypes)
                    .anyMatch(type -> type.equals(emailAttachment.getMimeType()));
            
            if (!isValidType) {
                throw new IllegalArgumentException("File type not allowed: " + emailAttachment.getMimeType());
            }
        }
        
        // Validate content ID format for inline attachments
        if (emailAttachment.getInline() != null && emailAttachment.getInline() && 
            emailAttachment.getCid() != null && !emailAttachment.getCid().matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException("Invalid content ID format. Must contain only alphanumeric characters, dots, underscores, and hyphens");
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
    
    /**
     * Detects if the provided content is HTML by checking for common HTML tags and structure
     */
    private boolean isHtmlContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        content = content.trim().toLowerCase();
        
        // Check for HTML doctype declaration
        if (content.startsWith("<!doctype html") || content.startsWith("<!doctype html>")) {
            return true;
        }
        
        // Check for opening HTML tag
        if (content.startsWith("<html")) {
            return true;
        }
        
        // Check for common HTML tags that indicate HTML content
        String[] htmlTags = {
            "<html", "<head", "<body", "<div", "<p>", "<br>", "<br/>", "<br />",
            "<span", "<h1", "<h2", "<h3", "<h4", "<h5", "<h6", "<ul", "<ol", "<li",
            "<table", "<tr", "<td", "<th", "<img", "<a", "<strong", "<b>", "<em", "<i>",
            "<style", "<script", "<meta", "<title", "<link"
        };
        
        for (String tag : htmlTags) {
            if (content.contains(tag)) {
                return true;
            }
        }
        
        // Check for HTML entities
        if (content.contains("&lt;") || content.contains("&gt;") || 
            content.contains("&amp;") || content.contains("&nbsp;") || 
            content.contains("&quot;")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Determine the effective sender address based on tenant configuration
     */
    private String determineSenderAddress(String requestedFrom, EmailTenant tenant) {
        if (tenant == null) {
            // No tenant - use provided from address (for individual users)
            return requestedFrom;
        }
        
        if (tenant.getDomainVerified() && tenant.hasVerifiedDomains()) {
            // Tenant has verified domains - validate against allowed domains
            try {
                String domain = emailTenantService.extractDomainFromEmail(requestedFrom);
                boolean isAllowedDomain = emailTenantService.isDomainVerifiedForTenant(requestedFrom, tenant);
                
                if (isAllowedDomain) {
                    log.debug("Using requested sender address {} for tenant {}", requestedFrom, tenant.getTenantCode());
                    return requestedFrom; // Use requested address
                } else {
                    log.info("Requested domain {} not verified for tenant {}, using default sender", 
                           domain, tenant.getTenantCode());
                    // Fall back to tenant's default sender
                    return tenant.getFormattedDefaultSender();
                }
            } catch (Exception e) {
                log.warn("Invalid email format {}, using tenant default sender", requestedFrom);
                return tenant.getFormattedDefaultSender();
            }
        } else {
            // Tenant without verified domain - use tenant default
            log.debug("Using tenant default sender for {} (no verified domains)", tenant.getTenantCode());
            return tenant.getFormattedDefaultSender();
        }
    }
    
    /**
     * Determine the effective reply-to address based on tenant configuration
     */
    private String determineReplyToAddress(String requestedFrom, EmailTenant tenant) {
        if (tenant == null) {
            // No tenant - no specific reply-to address
            return null;
        }
        
        if (tenant.getDomainVerified() && tenant.hasVerifiedDomains()) {
            // Tenant has verified domains - try to find domain-specific reply-to
            try {
                String domain = emailTenantService.extractDomainFromEmail(requestedFrom);
                
                // Look for domain-specific reply-to configuration
                if (tenant.getAllowedDomains() != null) {
                    for (var emailDomain : tenant.getAllowedDomains()) {
                        if (emailDomain.getDomain().equalsIgnoreCase(domain) && 
                            emailDomain.getIsVerified() &&
                            emailDomain.getDomainReplyToEmail() != null) {
                            log.debug("Using domain-specific reply-to for domain {}: {}", 
                                     domain, emailDomain.getFormattedReplyTo());
                            return emailDomain.getFormattedReplyTo();
                        }
                    }
                }
                
                // Fall back to tenant default reply-to
                log.debug("Using tenant default reply-to for {}: {}", 
                         tenant.getTenantCode(), tenant.getFormattedDefaultReplyTo());
                return tenant.getFormattedDefaultReplyTo();
            } catch (Exception e) {
                log.warn("Error determining reply-to for {}, using tenant default", requestedFrom);
                return tenant.getFormattedDefaultReplyTo();
            }
        } else {
            // Tenant without verified domain - use tenant default reply-to
            log.debug("Using tenant default reply-to for {} (no verified domains): {}", 
                     tenant.getTenantCode(), tenant.getFormattedDefaultReplyTo());
            return tenant.getFormattedDefaultReplyTo();
        }
    }
    
    /**
     * Validate sender domain against tenant's verified domains
     */
    private void validateSenderDomain(String senderAddress, EmailTenant tenant) {
        if (tenant == null || !tenant.getDomainVerified()) {
            return; // No validation needed
        }
        
        try {
            String domain = emailTenantService.extractDomainFromEmail(senderAddress);
            if (!emailTenantService.isDomainVerifiedForTenant(senderAddress, tenant)) {
                throw new SecurityException(
                    String.format("Domain %s is not verified for tenant %s", domain, tenant.getTenantCode()));
            }
        } catch (IllegalArgumentException e) {
            throw new SecurityException("Invalid sender email format: " + senderAddress);
        }
    }
}
