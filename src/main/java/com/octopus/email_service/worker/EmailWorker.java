package com.octopus.email_service.worker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.octopus.email_service.entity.Attachment;
import com.octopus.email_service.entity.Email;
import com.octopus.email_service.entity.Template;
import com.octopus.email_service.enums.BodyType;
import com.octopus.email_service.enums.EmailStatus;
import com.octopus.email_service.repository.EmailRepository;
import com.octopus.email_service.service.AttachmentService;
import com.octopus.email_service.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailWorker {
    
    private final EmailRepository emailRepository;
    private final EmailService emailService;
    private final AttachmentService attachmentService;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;
    
    @Value("${app.email.max-attempts:5}")
    private int maxAttempts;
    
    @Value("${app.email.retry-delay:60000}")
    private long retryDelay;
    
    @RabbitListener(queues = "${app.email.queue.name:email.queue}")
    @Transactional
    public void processEmail(Long emailId) {
        log.info("Processing email ID: {}", emailId);
        
        Email email = emailRepository.findById(emailId).orElse(null);
        if (email == null) {
            log.error("Email not found with ID: {} - possible race condition, will retry", emailId);
            // Sleep briefly and retry once in case of race condition
            try {
                Thread.sleep(100);
                email = emailRepository.findById(emailId).orElse(null);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            
            if (email == null) {
                log.error("Email still not found with ID: {} after retry - skipping", emailId);
                return;
            }
        }
        
        // Check if email has exceeded max attempts
        if (email.getAttempts() >= email.getMaxAttempts()) {
            log.error("Email ID {} has exceeded max attempts ({}), moving to DLQ", 
                     emailId, email.getMaxAttempts());
            emailService.updateEmailStatus(emailId, EmailStatus.FAILED, 
                                         "Exceeded max attempts: " + email.getMaxAttempts());
            return;
        }
        
        try {
            // Update status to SENDING
            emailService.updateEmailStatus(emailId, EmailStatus.SENDING, null);
            
            // Process the email
            sendEmail(email);
            
            // Mark as sent
            emailService.markAsSent(emailId);
            log.info("Successfully sent email ID: {} to {}", emailId, email.getToAddress());
            
        } catch (Exception e) {
            log.error("Failed to send email ID: {}", emailId, e);
            
            // Update status with error
            emailService.updateEmailStatus(emailId, EmailStatus.FAILED, e.getMessage());
            
            // If not at max attempts, schedule retry
            if (email.getAttempts() + 1 < email.getMaxAttempts()) {
                scheduleRetry(emailId);
            }
        }
    }
    
    private void sendEmail(Email email) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        
        // Set basic email properties
        helper.setFrom(email.getFromAddress());
        helper.setTo(email.getToAddress());
        
        // Set reply-to address if provided
        if (email.getReplyToAddress() != null && !email.getReplyToAddress().trim().isEmpty()) {
            helper.setReplyTo(email.getReplyToAddress());
            log.debug("Set reply-to address: {}", email.getReplyToAddress());
        }
        
        if (email.getCcAddresses() != null && !email.getCcAddresses().isEmpty()) {
            helper.setCc(email.getCcAddresses().toArray(new String[0]));
        }
        
        if (email.getBccAddresses() != null && !email.getBccAddresses().isEmpty()) {
            helper.setBcc(email.getBccAddresses().toArray(new String[0]));
        }
        
        // Process subject and body
        String subject = email.getSubject();
        String body = email.getBody();
        boolean isHtml = false;
        
        // If template is used, render it
        if (email.getTemplate() != null) {
            Template template = email.getTemplate();
            Map<String, Object> templateVars = parseTemplateVars(email.getTemplateVars());
            
            // Render subject
            if (template.getSubjectTemplate() != null) {
                Context context = new Context();
                context.setVariables(templateVars);
                subject = templateEngine.process(template.getSubjectTemplate(), context);
            }
            
            // Render body
            if (template.getBodyTemplate() != null) {
                Context context = new Context();
                context.setVariables(templateVars);
                body = templateEngine.process(template.getBodyTemplate(), context);
            }
            
            // Set HTML flag based on template body type
            isHtml = template.getBodyType() == BodyType.HTML;
        }
        // Check if we need to use fallback template for plain text body
        else if (email.getNeedsFallbackTemplate() != null && email.getNeedsFallbackTemplate()) {
            // Use classic template as fallback for plain text content
            Context context = new Context();
            context.setVariable("subject", subject);
            context.setVariable("body", body);
            
            // Add any additional template variables if provided
            Map<String, Object> templateVars = parseTemplateVars(email.getTemplateVars());
            context.setVariables(templateVars);
            
            // Render using the classic email template
            body = templateEngine.process("email/classic-email", context);
            isHtml = true; // Classic template produces HTML
            
            log.debug("Applied classic fallback template for email ID: {}", email.getId());
        }
        // For HTML body content without template
        else if (email.getIsHtmlBody() != null && email.getIsHtmlBody()) {
            isHtml = true;
        }
        
        helper.setSubject(subject);
        helper.setText(body, isHtml);
        
        // Add tracking headers
        helper.getMimeMessage().setHeader("X-Email-ID", email.getUuid().toString());
        helper.getMimeMessage().setHeader("List-Unsubscribe", "<mailto:unsubscribe@example.com>");
        
        // Process attachments
        processAttachments(helper, email.getAttachments());
        
        // Send the email
        mailSender.send(mimeMessage);
    }
    
    private Map<String, Object> parseTemplateVars(String templateVarsJson) {
        if (templateVarsJson == null || templateVarsJson.trim().isEmpty()) {
            return Map.of();
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(templateVarsJson, Map.class);
            return result;
        } catch (Exception e) {
            log.error("Failed to parse template variables", e);
            return Map.of();
        }
    }
    
    private void scheduleRetry(Long emailId) {
        // For now, we'll just log the retry. In a production system, you might want to
        // use a delayed queue or scheduler to retry after a delay
        log.info("Scheduling retry for email ID: {} after {} ms", emailId, retryDelay);
        
        // TODO: Implement proper retry mechanism with exponential backoff
        // This could involve:
        // 1. Using RabbitMQ delayed message plugin
        // 2. Using a separate retry queue with TTL
        // 3. Using Spring's @Scheduled annotation with a retry table
    }
    
    private void processAttachments(MimeMessageHelper helper, String attachmentsJson) {
        if (attachmentsJson == null || attachmentsJson.trim().isEmpty()) {
            return;
        }
        
        try {
            List<Attachment> attachments = objectMapper.readValue(
                    attachmentsJson, 
                    new TypeReference<List<Attachment>>() {}
            );
            
            for (Attachment attachment : attachments) {
                try {
                    // Download attachment content
                    InputStream attachmentStream = attachmentService.downloadAttachment(attachment.getId());
                    byte[] attachmentBytes = attachmentStream.readAllBytes();
                    attachmentStream.close();
                    
                    // Determine display name
                    String displayName = attachment.getOriginalFilename();
                    
                    // Add attachment to email (inline or regular)
                    if (attachment.getIsInline() != null && attachment.getIsInline() && attachment.getContentId() != null) {
                        // Add as inline attachment with content ID
                        helper.addInline(attachment.getContentId(), new ByteArrayResource(attachmentBytes), attachment.getContentType());
                        log.debug("Added inline attachment {} with CID {} to email", displayName, attachment.getContentId());
                    } else {
                        // Add as regular attachment
                        helper.addAttachment(displayName, new ByteArrayResource(attachmentBytes), attachment.getContentType());
                        log.debug("Added attachment {} to email", displayName);
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to process attachment {}: {}", attachment.getId(), e.getMessage(), e);
                    // Continue processing other attachments even if one fails
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to parse attachments: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process attachments", e);
        }
    }
}
