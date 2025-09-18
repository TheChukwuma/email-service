package com.octopus.email_service.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.octopus.email_service.entity.Email;
import com.octopus.email_service.entity.Template;
import com.octopus.email_service.enums.BodyType;
import com.octopus.email_service.enums.EmailStatus;
import com.octopus.email_service.repository.EmailRepository;
import com.octopus.email_service.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailWorker {
    
    private final EmailRepository emailRepository;
    private final EmailService emailService;
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
            log.error("Email not found with ID: {}", emailId);
            return;
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
        
        if (email.getCcAddresses() != null && !email.getCcAddresses().isEmpty()) {
            helper.setCc(email.getCcAddresses().toArray(new String[0]));
        }
        
        if (email.getBccAddresses() != null && !email.getBccAddresses().isEmpty()) {
            helper.setBcc(email.getBccAddresses().toArray(new String[0]));
        }
        
        // Process subject and body
        String subject = email.getSubject();
        String body = email.getBody();
        
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
        }
        
        helper.setSubject(subject);
        
        // Set body based on template type
        if (email.getTemplate() != null && email.getTemplate().getBodyType() == BodyType.HTML) {
            helper.setText(body, true);
        } else {
            helper.setText(body, false);
        }
        
        // Add tracking headers
        helper.getMimeMessage().setHeader("X-Email-ID", email.getUuid().toString());
        helper.getMimeMessage().setHeader("List-Unsubscribe", "<mailto:unsubscribe@example.com>");
        
        // TODO: Add attachments processing
        // processAttachments(helper, email.getAttachments());
        
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
}
